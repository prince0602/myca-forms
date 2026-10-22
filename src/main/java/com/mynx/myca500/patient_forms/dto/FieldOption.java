package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.Size;

public record FieldOption(
         @Size(max = 100) String id,
         @Size(max = 255) String label) {
}
