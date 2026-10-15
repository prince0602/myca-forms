package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FormTemplatePayload(
        @NotBlank(message = "Template name is required") @Size(max = 255) String label,
        @Size(max = 2000) String description,
        Integer expirationHours,
        @Valid List<FormFieldDto> fields) {

    public FormTemplatePayload {
        label = label == null ? null : label.trim();
        description = description == null ? "" : description;
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
