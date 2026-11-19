package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.EmailDto;
import com.mynx.myca500.patient_forms.dto.EmailSentResponse;
import com.mynx.myca500.patient_forms.dto.FormLinkResponse;
import com.mynx.myca500.patient_forms.dto.SendFormLinkDto;
import com.mynx.myca500.patient_forms.service.EmailService;
import com.mynx.myca500.patient_forms.service.FormAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final FormAssignmentService formAssignmentService;

    @PostMapping("/send")
    public EmailSentResponse send(@Valid @RequestBody EmailDto request) {
        return emailService.send(request);
    }

    /** Kept for existing clients; same behaviour as POST /api/v1/form-templates/{id}/links with an email. */
    @PostMapping("/send-form-link")
    public EmailSentResponse sendFormLink(@Valid @RequestBody SendFormLinkDto request,
                                          @AuthenticationPrincipal Jwt jwt) {
        FormLinkResponse created = formAssignmentService.createLink(request.templateId(), request.patientId(),
                request.email(), request.expiresInHours(), jwt.getSubject());
        return new EmailSentResponse(created.emailMessageId());
    }

}
