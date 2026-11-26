package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.dto.Paginated;
import com.mynx.myca500.patient_forms.dto.SentFormListItem;
import com.mynx.myca500.patient_forms.dto.SentFormPreview;
import com.mynx.myca500.patient_forms.entity.FormResponse;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import com.mynx.myca500.patient_forms.entity.Recipient;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.enums.SentFormStatus;
import com.mynx.myca500.patient_forms.exception.ResourceNotFoundException;
import com.mynx.myca500.patient_forms.repository.FormResponseRepository;
import com.mynx.myca500.patient_forms.repository.FormTemplateRepository;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.repository.RecipientRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SentFormQueryService {

    private final FormTemplateRepository templates;
    private final FormTemplateVersionRepository versions;
    private final SentFormRepository sentForms;
    private final FormResponseRepository responses;
    private final ImageUrlResolver imageUrls;
    private final RecipientRepository recipients;
    private final FormUploadUrlResolver uploadUrls;


    public Paginated<SentFormListItem> list(UUID templateId, String recipientId, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page - 1, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "sentAt"));

        Page<SentForm> result = recipientId == null || recipientId.isBlank()
                ? sentForms.findByTemplateId(templateId, pageable)
                : sentForms.findByTemplateIdAndRecipientId(templateId, recipientId.trim(), pageable);


        if (result.isEmpty() && !templates.existsById(templateId)) {
            throw new ResourceNotFoundException("Template not found: " + templateId);
        }

        Instant now = Instant.now();
        Map<String, String> names = recipientNames(result.getContent().stream().map(SentForm::getRecipientId).toList());
        List<SentFormListItem> items = result.getContent().stream()
                .map(s -> toItem(s, names.get(s.getRecipientId()), now)).toList();
        return new Paginated<>(items, result.getTotalElements());
    }


    public SentFormPreview preview(UUID templateId, UUID sentFormId) {
        SentForm form = sentForms.findById(sentFormId)
                .filter(s -> s.getTemplateId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException("Sent form not found: " + sentFormId));

        FormTemplateVersion version = versions
                .findByTemplateIdAndVersionNumber(form.getTemplateId(), form.getTemplateVersion())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing template " + form.getTemplateId() + " v" + form.getTemplateVersion()));

        Optional<FormResponse> saved = form.getStatus() == SentFormStatus.IN_PROGRESS || form.getSubmittedAt() != null
                ? responses.findBySentFormId(form.getId())
                : Optional.empty();

        List<FormFieldDto> fields = imageUrls.resolve(version.getFields() == null ? List.of() : version.getFields());
        Map<String, Object> answers = saved.map(FormResponse::getAnswers).orElseGet(Map::of);
        return new SentFormPreview(
                toItem(form, recipientNames(List.of(form.getRecipientId())).get(form.getRecipientId()), Instant.now()),
                version.getLabel(),
                version.getDescription(),
                fields,
                answers,
                saved.map(FormResponse::getStatus).orElse(null),
                uploadUrls.resolve(form.getId(), answers));
    }

    /**
     * Recipient display names in one query. Sent forms from the legacy endpoints may hold ids that are not
     * recipient UUIDs; those simply get no name.
     */
    private Map<String, String> recipientNames(Collection<String> recipientIds) {
        List<UUID> ids = recipientIds.stream().distinct()
                .map(RecipientService::parseId).flatMap(Optional::stream).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return recipients.findAllById(ids).stream()
                .collect(Collectors.toMap(r -> r.getId().toString(), Recipient::fullName));
    }

    private static SentFormListItem toItem(SentForm s, String recipientName, Instant now) {
        return new SentFormListItem(
                s.getId().toString(),
                s.getRecipientId(),
                recipientName,
                s.getRecipientEmail(),
                s.getTemplateVersion(),
                effectiveStatus(s, now).getValue(),
                s.getSentAt(),
                s.getOpenedAt(),
                s.getSubmittedAt(),
                s.getExpiresAt(),
                s.getRevokedAt(),
                s.getCreatedBy());
    }

    private static SentFormStatus effectiveStatus(SentForm s, Instant now) {
        if (s.getSubmittedAt() != null) return SentFormStatus.SUBMITTED;
        if (s.getRevokedAt() != null) return SentFormStatus.REVOKED;
        if (s.getExpiresAt() != null && !now.isBefore(s.getExpiresAt())) return SentFormStatus.EXPIRED;
        return s.getStatus();
    }
}
