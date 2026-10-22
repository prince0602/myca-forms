package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DiagramPoint(
        @Size(max = 100) String id,
        @Size(max = 255) String label,
        @NotNull Double x,
        @NotNull Double y) {

    public DiagramPoint {
        id = (id == null || id.isBlank()) ? "pt-" + UUID.randomUUID() : id;
        label = label == null ? "" : label;
    }
}
