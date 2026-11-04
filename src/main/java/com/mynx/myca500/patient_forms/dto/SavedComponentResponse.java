package com.mynx.myca500.patient_forms.dto;

import java.time.Instant;
import java.util.List;

public record SavedComponentResponse(
        String id,
        String name,
        DocumentContent content,
        List<FormFieldDto> fields,
        String createdBy,
        Instant createdAt) {
}
