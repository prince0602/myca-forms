package com.mynx.myca500.patient_forms.dto;

import java.time.Instant;

public record SubmitFormResult(
        String status,
        Instant submittedAt,
        int templateVersion) {
}
