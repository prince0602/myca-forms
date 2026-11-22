package com.mynx.myca500.patient_forms.dto;

import java.time.Instant;

public record SaveDraftResult(
        String status,
        Instant savedAt,
        long lockVersion) {
}
