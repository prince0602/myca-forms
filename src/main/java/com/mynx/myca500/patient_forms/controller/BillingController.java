package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.request.UpgradePlanRequest;
import com.mynx.myca500.patient_forms.dto.response.WorkspaceUsageResponse;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.service.BillingService;
import com.mynx.myca500.patient_forms.service.CurrentUserService;
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
@RequestMapping("/api/workspaces/{workspaceId}")
@RequiredArgsConstructor
public class BillingController {

    private final CurrentUserService currentUserService;
    private final BillingService billingService;

    @GetMapping("/usage")
    public WorkspaceUsageResponse usage(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId) {
        User user = currentUserService.resolve(jwt);
        return billingService.getUsage(user, workspaceId);
    }

    @PostMapping("/billing/upgrade")
    public WorkspaceUsageResponse upgrade(@AuthenticationPrincipal Jwt jwt, @PathVariable String workspaceId,
                                          @Valid @RequestBody UpgradePlanRequest request) {
        User user = currentUserService.resolve(jwt);
        return billingService.upgrade(user, workspaceId, request);
    }
}
