package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.EmailDto;
import com.mynx.myca500.patient_forms.dto.EmailSentResponse;
import com.mynx.myca500.patient_forms.dto.SendFormLinkDto;
import com.mynx.myca500.patient_forms.entity.FormTemplate;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.enums.SentFormStatus;
import com.mynx.myca500.patient_forms.enums.TemplateStatus;
import com.mynx.myca500.patient_forms.exception.ResourceNotFoundException;
import com.mynx.myca500.patient_forms.exception.ValidationException;
import com.mynx.myca500.patient_forms.repository.FormTemplateRepository;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FormEmailService {

    private final EmailService emailService;
    private final FormLinkService formLinkService;
    private final SentFormRepository sentForms;
    private final FormTemplateRepository templates;
    private final FormTemplateVersionRepository versions;

    @Value("${app.environment-url}")
    private String environmentUrl;

    /** Used when neither the request nor the template version sets an expiry. */
    @Value("${forms.link.default-expiration-hours:72}")
    private int defaultExpirationHours;

    /** Same upper bound the template validator applies to expirationHours. */
    @Value("${forms.template.max-expiration-hours:8760}")
    private int maxExpirationHours;

    @Transactional
    public EmailSentResponse sendFormLink(SendFormLinkDto dto, String createdBy) {
        FormTemplate template = templates.findById(dto.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + dto.templateId()));
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new ValidationException("Template is not active: " + dto.templateId());
        }

        // Pin the link to the template's current version on the server (B10).
        int pinnedVersion = template.getCurrentVersion();
        FormTemplateVersion version = versions.findByTemplateIdAndVersionNumber(template.getId(), pinnedVersion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template " + template.getId() + " has no version " + pinnedVersion));

        Instant now = Instant.now();
        Instant expiresAt = now.plus(resolveExpirationHours(dto.expiresInHours(), version), ChronoUnit.HOURS);

        SentForm sf = new SentForm();
        sf.setTemplateId(template.getId());
        sf.setTemplateVersion(pinnedVersion);
        sf.setPatientId(dto.patientId());
        sf.setRecipientEmail(dto.email());
        sf.setStatus(SentFormStatus.SENT);
        sf.setSentAt(now);
        sf.setExpiresAt(expiresAt);
        sf.setCreatedBy(createdBy);
        sentForms.save(sf);

        String token = formLinkService.create(sf.getId(), expiresAt);
        String link = environmentUrl + "/forms/fill?token=" + token;

        return emailService.send(EmailDto.fromTemplate(dto.email(), "form-request", Map.of("link", link)));
    }

    /** Request value, then the version's expirationHours, then the configured default (B8). */
    private int resolveExpirationHours(Integer requested, FormTemplateVersion version) {
        int hours;
        if (requested != null) {
            hours = requested;
        } else if (version.getExpirationHours() != null) {
            hours = version.getExpirationHours();
        } else {
            hours = defaultExpirationHours;
        }
        if (hours < 1 || hours > maxExpirationHours) {
            throw new ValidationException(
                    "Expiration must be a whole number of hours between 1 and " + maxExpirationHours);
        }
        return hours;
    }
}
