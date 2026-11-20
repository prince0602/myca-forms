package com.mynx.myca500.patient_forms.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SwitchWorkspaceRequest(@NotBlank String workspaceId) {
}
