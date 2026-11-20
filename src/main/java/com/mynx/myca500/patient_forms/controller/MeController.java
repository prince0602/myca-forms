package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.request.SwitchWorkspaceRequest;
import com.mynx.myca500.patient_forms.dto.response.MeResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.service.CurrentUserService;
import com.mynx.myca500.patient_forms.service.MeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserService currentUserService;
    private final MeService meService;

    @GetMapping
    public MeResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        User user = currentUserService.resolve(jwt);
        return meService.getMe(user);
    }

    @PutMapping("/default-workspace")
    public MeResponse switchWorkspace(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody SwitchWorkspaceRequest request) {
        User user = currentUserService.resolve(jwt);
        return meService.switchWorkspace(user.getId(), request.workspaceId());
    }

}
