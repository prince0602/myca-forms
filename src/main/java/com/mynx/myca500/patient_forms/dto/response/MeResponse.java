package com.mynx.myca500.patient_forms.dto.response;

import com.mynx.myca500.patient_forms.enums.OnboardingStatus;

import java.util.List;

public record MeResponse(
        String accountId,
        String firstName,
        String lastName,
        String email,
        OnboardingStatus onboarding,
        List<WorkspaceSummaryResponse> workspaces,
        String currentWorkspaceId) {
}
