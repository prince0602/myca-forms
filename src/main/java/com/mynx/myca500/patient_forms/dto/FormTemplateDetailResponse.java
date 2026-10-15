package com.mynx.myca500.patient_forms.dto;


import com.mynx.myca500.patient_forms.enums.TemplateStatus;

import java.time.Instant;
import java.util.List;

public record FormTemplateDetailResponse(
        String id,
        String label,
        String description,
        TemplateStatus status,
        int version,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt,
        Integer expirationHours,
        List<FormFieldDto> fields) {
}
