package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.EmailDto;
import com.mynx.myca500.patient_forms.dto.EmailSentResponse;
import com.mynx.myca500.patient_forms.dto.SendFormLinkDto;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.enums.SentFormStatus;
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
    @Value("${app.environment-url}")
    private String environmentUrl;

    @Transactional
    public EmailSentResponse sendFormLink(SendFormLinkDto dto, String createdBy) {
        int hours = dto.expiresInHours() == null ? 72 : dto.expiresInHours();
        Instant expiresAt = Instant.now().plus(hours, ChronoUnit.HOURS);

        SentForm sf = new SentForm();
        sf.setTemplateId(dto.templateId());
        sf.setTemplateVersion(dto.templateVersion());
        sf.setPatientId(dto.patientId());
        sf.setRecipientEmail(dto.email());
        sf.setStatus(SentFormStatus.SENT);
        sf.setSentAt(Instant.now());
        sf.setExpiresAt(expiresAt);
        sf.setCreatedBy(createdBy);
        sentForms.save(sf);

        String token = formLinkService.create(sf.getId(), expiresAt);
        String link = environmentUrl + "/forms/fill?token=" + token;

        return emailService.send(EmailDto.fromTemplate(dto.email(), "form-request", Map.of("link", link)));
    }
}
