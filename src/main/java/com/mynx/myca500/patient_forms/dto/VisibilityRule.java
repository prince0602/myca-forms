package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VisibilityRule(
        @NotBlank @Size(max = 100) String fieldId,
        @NotBlank @Size(max = 100) String optionId) {
}
