package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.request.SendFormToRecipientRequest;
import com.mynx.myca500.patient_forms.dto.response.RecipientResponse;
import com.mynx.myca500.patient_forms.dto.response.SendFormResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.service.CurrentUserService;
import com.mynx.myca500.patient_forms.service.RecipientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/recipients")
@RequiredArgsConstructor
public class RecipientController {

    private final CurrentUserService currentUserService;
    private final RecipientService recipientService;

    @PostMapping("/send-form")
    public SendFormResponse sendForm(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId,
                                     @Valid @RequestBody SendFormToRecipientRequest request) {
        User user = currentUserService.resolve(jwt);
        return recipientService.sendForm(user, workspaceId, request, jwt.getSubject());
    }

    @GetMapping("/{recipientId}")
    public RecipientResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId,
                                 @PathVariable String recipientId) {
        User user = currentUserService.resolve(jwt);
        return recipientService.get(user, workspaceId, recipientId);
    }
}
