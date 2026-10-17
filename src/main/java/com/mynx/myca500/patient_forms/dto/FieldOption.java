package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FieldOption(
        @NotBlank @Size(max = 100) String id,
        @NotBlank @Size(max = 255) String label) {
}
