package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.EmailDto;
import com.mynx.myca500.patient_forms.dto.EmailSentResponse;
import com.mynx.myca500.patient_forms.dto.FormLinkResponse;
import com.mynx.myca500.patient_forms.entity.FormTemplate;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import com.mynx.myca500.patient_forms.entity.Recipient;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.enums.SentFormStatus;
import com.mynx.myca500.patient_forms.enums.TemplateStatus;
import com.mynx.myca500.patient_forms.exception.ResourceNotFoundException;
import com.mynx.myca500.patient_forms.exception.ValidationException;
import com.mynx.myca500.patient_forms.repository.FormTemplateRepository;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.repository.RecipientRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormAssignmentService {

    private static final String EMAIL_TEMPLATE = "form-request";

    private final FormTemplateRepository templates;
    private final FormTemplateVersionRepository versions;
    private final SentFormRepository sentForms;
    private final FormLinkService formLinkService;
    private final EmailService emailService;
    private final RecipientRepository recipients;
    private final FormUsageService formUsageService;

    @Value("${app.environment-url}")
    private String environmentUrl;


    @Value("${forms.link.default-expiration-hours:72}")
    private int defaultExpirationHours;

    @Value("${forms.template.max-expiration-hours:8760}")
    private int maxExpirationHours;

    @Transactional
    public FormLinkResponse createLink(UUID templateId, String recipientId, String email,
                                       Integer expiresInHours, String createdBy) {
        return doCreateLink(templateId, recipientId, email, expiresInHours, createdBy);
    }

    @Transactional
    public FormLinkResponse createLinkForWorkspace(UUID workspaceId, UUID templateId, String recipientId,
                                                   String email, Integer expiresInHours, String createdBy) {
        formUsageService.consumeOne(workspaceId);
        return doCreateLink(templateId, recipientId, email, expiresInHours, createdBy);
    }

    private FormLinkResponse doCreateLink(UUID templateId, String recipientId, String email,
                                          Integer expiresInHours, String createdBy) {
        FormTemplate template = templates.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new ValidationException("Template is not active: " + templateId);
        }

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
        sf.setRecipientId(recipientId);
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


    public String resolveRecipientEmail(String recipientId, String email) {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return RecipientService.parseId(recipientId)
                .flatMap(recipients::findById)
                .map(Recipient::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found: " + recipientId));
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
