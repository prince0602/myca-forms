package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.dto.PublicFormResponse;
import com.mynx.myca500.patient_forms.entity.FormLink;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.exception.FormLinkException;
import com.mynx.myca500.patient_forms.exception.FormLinkException.Code;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class PublicFormService {

    private static final int MAX_TOKEN_LENGTH = 200;

    private final FormLinkService linkService;
    private final SentFormRepository sentForms;
    private final FormTemplateVersionRepository versions;


    @Transactional
    public PublicFormResponse open(String token) {
        if (token == null || token.isBlank()) {
            throw new FormLinkException(Code.TOKEN_MISSING);
        }
        String trimmed = token.trim();
        if (trimmed.length() > MAX_TOKEN_LENGTH) {
            throw new FormLinkException(Code.LINK_INVALID);
        }
        FormLink link = linkService.validate(trimmed);

        SentForm form = sentForms.findById(link.getSentFormId())
                .orElseThrow(() -> new FormLinkException(Code.LINK_INVALID));

        Instant now = Instant.now();
        if (form.getRevokedAt() != null) {
            throw new FormLinkException(Code.LINK_REVOKED);
        }
        if (form.getExpiresAt() != null && !now.isBefore(form.getExpiresAt())) {
            throw new FormLinkException(Code.LINK_EXPIRED);
        }

        sentForms.markOpened(form.getId(), now);
        SentForm opened = sentForms.findById(form.getId())
                .orElseThrow(() -> new FormLinkException(Code.LINK_INVALID));

        FormTemplateVersion version = versions
                .findByTemplateIdAndVersionNumber(opened.getTemplateId(), opened.getTemplateVersion())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing template " + opened.getTemplateId() + " v" + opened.getTemplateVersion()));

        boolean submitted = opened.getSubmittedAt() != null || link.getUsedAt() != null;

        return new PublicFormResponse(
                version.getLabel(),
                version.getDescription(),
                opened.getTemplateVersion(),
                label(opened.getStatus().name()),
                submitted,
                link.getExpiresAt() != null ? link.getExpiresAt() : opened.getExpiresAt(),
                opened.getSubmittedAt(),
                fieldsOf(version),
                answersOf(opened),
                lockVersionOf(opened));
    }

    private static List<FormFieldDto> fieldsOf(FormTemplateVersion version) {
        return version.getFields() == null ? List.of() : version.getFields();
    }

    private Map<String, Object> answersOf(SentForm form) {
        return Map.of();
    }
    private Long lockVersionOf(SentForm form) {
        return null;
    }

    private static String label(String status) {
        String lower = status.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}