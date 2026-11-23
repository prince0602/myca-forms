package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.request.CreateInvitationRequest;
import com.mynx.myca500.patient_forms.dto.response.InvitationResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.service.CurrentUserService;
import com.mynx.myca500.patient_forms.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final CurrentUserService currentUserService;
    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponse> create(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId,
                                                     @Valid @RequestBody CreateInvitationRequest request) {
        User user = currentUserService.resolve(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.create(user, workspaceId, request.email()));
    }

    @GetMapping
    public List<InvitationResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId) {
        User user = currentUserService.resolve(jwt);
        return invitationService.listPending(user, workspaceId);
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId,
                                       @PathVariable String invitationId) {
        User user = currentUserService.resolve(jwt);
        invitationService.revoke(user, workspaceId, invitationId);
        return ResponseEntity.noContent().build();
    }
}
