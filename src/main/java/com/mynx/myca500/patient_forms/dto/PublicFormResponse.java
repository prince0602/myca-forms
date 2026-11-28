package com.mynx.myca500.patient_forms.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PublicFormResponse(
        String title,
        String description,
        int templateVersion,
        String status,
        boolean readOnly,
        Instant expiresAt,
        Instant submittedAt,
        List<FormFieldDto> fields,
        Map<String, Object> answers,
        Long lockVersion,
        Map<String, String> fileUrls,
        String pdfUrl
) {}
