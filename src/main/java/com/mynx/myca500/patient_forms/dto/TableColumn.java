package com.mynx.myca500.patient_forms.dto;

import com.mynx.myca500.patient_forms.enums.ColumnType;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TableColumn(
        @Size(max = 100) String id,
        @Size(max = 255) String label,
        ColumnType type) {

    public TableColumn {
        id = (id == null || id.isBlank()) ? "col-" + UUID.randomUUID() : id;
        label = label == null ? "" : label;
        type = type == null ? ColumnType.TEXT : type;
    }
}
