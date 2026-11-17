package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.EmailDto;
import com.mynx.myca500.patient_forms.dto.EmailSentResponse;
import com.mynx.myca500.patient_forms.dto.SendFormLinkDto;
import com.mynx.myca500.patient_forms.service.EmailService;
import com.mynx.myca500.patient_forms.service.FormEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final FormEmailService formEmailService;

    @PostMapping("/send")
    public EmailSentResponse send(@Valid @RequestBody EmailDto request) {
        return emailService.send(request);
    }

    @PostMapping("/send-form-link")
    public EmailSentResponse sendFormLink(@Valid @RequestBody SendFormLinkDto request,
                                          @AuthenticationPrincipal Jwt jwt) {
        return formEmailService.sendFormLink(request, jwt.getSubject());
    }

}
