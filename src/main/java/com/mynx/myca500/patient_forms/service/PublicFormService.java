package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.dto.PublicFormResponse;
import com.mynx.myca500.patient_forms.dto.PublicUploadResponse;
import com.mynx.myca500.patient_forms.dto.SaveDraftRequest;
import com.mynx.myca500.patient_forms.dto.SaveDraftResult;
import com.mynx.myca500.patient_forms.dto.SubmitFormRequest;
import com.mynx.myca500.patient_forms.dto.SubmitFormResult;
import com.mynx.myca500.patient_forms.entity.FormLink;
import com.mynx.myca500.patient_forms.entity.FormResponse;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.enums.SentFormStatus;
import com.mynx.myca500.patient_forms.event.FormSubmittedEvent;
import com.mynx.myca500.patient_forms.exception.FormLinkException;
import com.mynx.myca500.patient_forms.exception.FormLinkException.Code;
import com.mynx.myca500.patient_forms.exception.ValidationException;
import com.mynx.myca500.patient_forms.repository.FormResponseRepository;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import com.mynx.myca500.patient_forms.service.FormAccessResolver.FormAccess;
import com.mynx.myca500.patient_forms.validation.AnswerValidator;
import com.mynx.myca500.patient_forms.validation.CompiledFormSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicFormService {

    private static final String UPLOAD_ROOT = "public-uploads/";
    private static final Set<String> UPLOAD_EXTENSIONS = Set.of("png", "jpg", "jpeg", "pdf");

    private final FormAccessResolver resolver;
    private final FormLinkService linkService;
    private final SentFormRepository sentForms;
    private final FormTemplateVersionRepository versions;
    private final FormResponseRepository responses;
    private final TemplateSchemaCache schemas;
    private final AnswerValidator validator;
    private final S3PresignedUrlService presigner;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;
    private final ImageUrlResolver imageUrls;


    @Transactional
    public PublicFormResponse open(String token) {
        FormAccess access = resolver.forRead(token);
        FormLink link = access.link();
        SentForm form = access.sentForm();

        Instant now = Instant.now();
        if (sentForms.markOpened(form.getId(), now) == 1) {
            form.setStatus(SentFormStatus.OPENED);
            form.setOpenedAt(now);
        }

        FormTemplateVersion version = versions
                .findByTemplateIdAndVersionNumber(form.getTemplateId(), form.getTemplateVersion())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing template " + form.getTemplateId() + " v" + form.getTemplateVersion()));

        Optional<FormResponse> saved = form.getStatus() == SentFormStatus.IN_PROGRESS || access.submitted()
                ? responses.findBySentFormId(form.getId())
                : Optional.empty();

        return new PublicFormResponse(
                version.getLabel(),
                version.getDescription(),
                form.getTemplateVersion(),
                form.getStatus().getValue(),
                access.submitted(),
                link.getExpiresAt() != null ? link.getExpiresAt() : form.getExpiresAt(),
                form.getSubmittedAt(),
                imageUrls.resolve(fieldsOf(version)),
                saved.map(FormResponse::getAnswers).orElseGet(Map::of),
                saved.map(FormResponse::getLockVersion).orElse(null));
    }



    public SaveDraftResult saveDraft(String token, SaveDraftRequest request) {
        FormAccess access = resolver.forWrite(token);
        SentForm form = access.sentForm();

        CompiledFormSchema schema = schemas.get(form.getTemplateId(), form.getTemplateVersion());
        Map<String, Object> answers = validator.validateDraft(schema, request.answers(), uploadPrefix(form.getId()));

        Instant now = Instant.now();
        try {
            long lockVersion = tx.execute(status -> persistDraft(form, request.lockVersion(), answers, now));
            return new SaveDraftResult(SentFormStatus.IN_PROGRESS.getValue(), now, lockVersion);
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
            throw new FormLinkException(Code.STALE_VERSION);
        }
    }

    private long persistDraft(SentForm form, Long lockVersion, Map<String, Object> answers, Instant now) {

        if (sentForms.markInProgress(form.getId(), now) == 0) {
            throw new FormLinkException(closedReason(form, now));
        }

        FormResponse response = responses.findBySentFormId(form.getId()).orElse(null);
        if (response == null) {
            response = new FormResponse();
            response.setSentFormId(form.getId());
            response.setTemplateId(form.getTemplateId());
            response.setTemplateVersion(form.getTemplateVersion());
        } else if (!"DRAFT".equals(response.getStatus())) {
            throw new FormLinkException(Code.ALREADY_SUBMITTED);
        } else if (lockVersion == null || response.getLockVersion() != lockVersion) {
            throw new FormLinkException(Code.STALE_VERSION);
        }
        response.setAnswers(answers);
        response.setStatus("DRAFT");

        return responses.saveAndFlush(response).getLockVersion();
    }

    private static Code closedReason(SentForm form, Instant now) {
        if (form.getExpiresAt() != null && !now.isBefore(form.getExpiresAt())) return Code.LINK_EXPIRED;
        if (form.getRevokedAt() != null) return Code.LINK_REVOKED;
        return Code.ALREADY_SUBMITTED;
    }

    public SubmitFormResult submit(String token, SubmitFormRequest request) {
        FormAccess access = resolver.forWrite(token);
        SentForm form = access.sentForm();

        CompiledFormSchema schema = schemas.get(form.getTemplateId(), form.getTemplateVersion());
        Map<String, Object> answers = validator.validate(schema, request.answers(), uploadPrefix(form.getId()));

        Instant now = Instant.now();
        try {
            tx.executeWithoutResult(status -> persist(access, request.lockVersion(), answers, now));
        } catch (DataIntegrityViolationException e) {
            throw new FormLinkException(Code.ALREADY_SUBMITTED);
        }

        log.info("Form submitted sentFormId={}", form.getId());
        return new SubmitFormResult(SentFormStatus.SUBMITTED.getValue(), now, form.getTemplateVersion());
    }

    private void persist(FormAccess access, Long lockVersion, Map<String, Object> answers, Instant now) {
        SentForm form = access.sentForm();

        if (sentForms.markSubmitted(form.getId(), now) == 0) {
            if (form.getExpiresAt() != null && !now.isBefore(form.getExpiresAt())) {
                throw new FormLinkException(Code.LINK_EXPIRED);
            }
            throw new FormLinkException(Code.ALREADY_SUBMITTED);
        }

        FormResponse response = responses.findBySentFormId(form.getId()).orElseGet(FormResponse::new);
        if (lockVersion != null && response.getId() != null && response.getLockVersion() != lockVersion) {
            throw new FormLinkException(Code.STALE_VERSION);
        }
        response.setSentFormId(form.getId());
        response.setTemplateId(form.getTemplateId());
        response.setTemplateVersion(form.getTemplateVersion());
        response.setAnswers(answers);
        response.setStatus("SUBMITTED");
        response.setSubmittedAt(now);
        FormResponse saved = responses.save(response);

        if (!linkService.markUsed(access.link().getId(), now)) {
            throw new FormLinkException(Code.ALREADY_SUBMITTED);
        }

        events.publishEvent(new FormSubmittedEvent(form.getId(), saved.getId(), now));
    }


    public PublicUploadResponse createUpload(String token, String extension) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!UPLOAD_EXTENSIONS.contains(ext)) {
            throw new ValidationException("Unsupported file type");
        }
        SentForm form = resolver.forWrite(token).sentForm();

        String folder = UPLOAD_ROOT + form.getId();
        String fileName = UUID.randomUUID() + "." + ext;
        String url = presigner.createPreSignedPutUrl(folder, fileName);
        return new PublicUploadResponse(folder + "/" + fileName, url);
    }

    static String uploadPrefix(UUID sentFormId) {
        return UPLOAD_ROOT + sentFormId + "/";
    }


    private static List<FormFieldDto> fieldsOf(FormTemplateVersion version) {
        return version.getFields() == null ? List.of() : version.getFields();
    }

}