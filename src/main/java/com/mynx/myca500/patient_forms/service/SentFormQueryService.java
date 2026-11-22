package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.dto.Paginated;
import com.mynx.myca500.patient_forms.dto.SentFormListItem;
import com.mynx.myca500.patient_forms.dto.SentFormPreview;
import com.mynx.myca500.patient_forms.entity.FormResponse;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.enums.SentFormStatus;
import com.mynx.myca500.patient_forms.exception.ResourceNotFoundException;
import com.mynx.myca500.patient_forms.repository.FormResponseRepository;
import com.mynx.myca500.patient_forms.repository.FormTemplateRepository;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SentFormQueryService {

    private final FormTemplateRepository templates;
    private final FormTemplateVersionRepository versions;
    private final SentFormRepository sentForms;
    private final FormResponseRepository responses;


    public Paginated<SentFormListItem> list(UUID templateId, String patientId, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page - 1, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "sentAt"));

        Page<SentForm> result = patientId == null || patientId.isBlank()
                ? sentForms.findByTemplateId(templateId, pageable)
                : sentForms.findByTemplateIdAndPatientId(templateId, patientId.trim(), pageable);


        if (result.isEmpty() && !templates.existsById(templateId)) {
            throw new ResourceNotFoundException("Template not found: " + templateId);
        }

        Instant now = Instant.now();
        List<SentFormListItem> items = result.getContent().stream().map(s -> toItem(s, now)).toList();
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

        List<FormFieldDto> fields = version.getFields() == null ? List.of() : version.getFields();
        return new SentFormPreview(
                toItem(form, Instant.now()),
                version.getLabel(),
                version.getDescription(),
                fields,
                saved.map(FormResponse::getAnswers).orElseGet(Map::of),
                saved.map(FormResponse::getStatus).orElse(null));
    }

    private static SentFormListItem toItem(SentForm s, Instant now) {
        return new SentFormListItem(
                s.getId().toString(),
                s.getPatientId(),
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
