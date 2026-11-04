package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SavedComponentRequest(
        @NotBlank(message = "Component name is required") @Size(max = 255) String name,
        @Valid DocumentContent content,
        @NotEmpty(message = "A component needs at least one field") @Valid List<FormFieldDto> fields) {

    public SavedComponentRequest {
        name = name == null ? null : name.trim();
        content = (content == null || content.blocks() == null) ? new DocumentContent(List.of()) : content;
    }
}