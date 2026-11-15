package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.EmailDto;
import com.mynx.myca500.patient_forms.dto.EmailSentResponse;
import com.mynx.myca500.patient_forms.dto.SendFormLinkDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.Map;

@Service
@RequiredArgsConstructor
public class FormEmailService {

    private final EmailService emailService;
    private final FormLinkService formLinkService;

    @Value("${app.environment-url}")
    private String environmentUrl;

    public EmailSentResponse sendFormLink(SendFormLinkDto dto) {
        int hours = dto.expiresInHours() == null ? 72 : dto.expiresInHours();
        String token = formLinkService.create(dto.formId(), dto.patientId(), hours);
        String link = environmentUrl + "/forms/fill?token=" + token;

        return emailService.send(EmailDto.fromTemplate(dto.email(), "form-request", Map.of("link", link)));
    }
}
