package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.EmailDto;
import com.mynx.myca500.patient_forms.dto.EmailSentResponse;
import com.mynx.myca500.patient_forms.dto.FormLinkResponse;
import com.mynx.myca500.patient_forms.entity.FormTemplate;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import com.mynx.myca500.patient_forms.entity.Patient;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.enums.SentFormStatus;
import com.mynx.myca500.patient_forms.enums.TemplateStatus;
import com.mynx.myca500.patient_forms.exception.ResourceNotFoundException;
import com.mynx.myca500.patient_forms.exception.ValidationException;
import com.mynx.myca500.patient_forms.repository.FormTemplateRepository;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.repository.PatientRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Assigns a form template to a patient: creates the {@link SentForm} pinned to the template's
 * current version, issues a {@code FormLink} token, and optionally emails the link.
 */
@Service
@RequiredArgsConstructor
public class FormAssignmentService {

    private static final String EMAIL_TEMPLATE = "form-request";

    private final FormTemplateRepository templates;
    private final FormTemplateVersionRepository versions;
    private final SentFormRepository sentForms;
    private final FormLinkService formLinkService;
    private final EmailService emailService;
    private final PatientRepository patients;

    @Value("${app.environment-url}")
    private String environmentUrl;

    /** Used when neither the request nor the template version sets an expiry. */
    @Value("${forms.link.default-expiration-hours:72}")
    private int defaultExpirationHours;

    /** Same upper bound the template validator applies to expirationHours. */
    @Value("${forms.template.max-expiration-hours:8760}")
    private int maxExpirationHours;

    /**
     * Creates a shareable link for {@code templateId}. If {@code email} is non-blank the link is
     * also emailed; an email failure rolls the whole assignment back.
     *
     * @param expiresInHours optional override; falls back to the version's expirationHours, then the default
     */
    @Transactional
    public FormLinkResponse createLink(UUID templateId, String patientId, String email,
                                       Integer expiresInHours, String createdBy) {
        FormTemplate template = templates.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new ValidationException("Template is not active: " + templateId);
        }

        // Pin the link to the template's current version on the server.
        int pinnedVersion = template.getCurrentVersion();
        FormTemplateVersion version = versions.findByTemplateIdAndVersionNumber(templateId, pinnedVersion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template " + templateId + " has no version " + pinnedVersion));

        Instant now = Instant.now();
        Instant expiresAt = now.plus(resolveExpirationHours(expiresInHours, version), ChronoUnit.HOURS);
        boolean sendEmail = email != null && !email.isBlank();

        SentForm sf = new SentForm();
        sf.setTemplateId(templateId);
        sf.setTemplateVersion(pinnedVersion);
        sf.setPatientId(patientId);
        sf.setRecipientEmail(sendEmail ? email : null);
        sf.setStatus(SentFormStatus.SENT);
        sf.setSentAt(now);
        sf.setExpiresAt(expiresAt);
        sf.setCreatedBy(createdBy);
        sentForms.save(sf);

        String token = formLinkService.create(sf.getId(), expiresAt);
        String link = environmentUrl + "/forms/fill?token=" + token;

        String messageId = null;
        if (sendEmail) {
            EmailSentResponse sent = emailService.send(
                    EmailDto.fromTemplate(email, EMAIL_TEMPLATE, Map.of("link", link)));
            messageId = sent.messageId();
        }

        return new FormLinkResponse(sf.getId().toString(), templateId.toString(), pinnedVersion,
                token, link, expiresAt, messageId);
    }


    public String resolveRecipientEmail(String patientId, String email) {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return PatientService.parseId(patientId)
                .flatMap(patients::findById)
                .map(Patient::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
    }


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
