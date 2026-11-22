package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.response.WorkspaceDetailsResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.service.CurrentUserService;
import com.mynx.myca500.patient_forms.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
@RequiredArgsConstructor
public class WorkspaceController {

    private final CurrentUserService currentUserService;
    private final WorkspaceService workspaceService;

    @GetMapping
    public WorkspaceDetailsResponse getWorkspace(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId) {
        User user = currentUserService.resolve(jwt);
        return workspaceService.getWorkspaceDetails(user, workspaceId);
    }
}
