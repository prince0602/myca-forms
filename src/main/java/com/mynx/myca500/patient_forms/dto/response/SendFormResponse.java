package com.mynx.myca500.patient_forms.dto.response;

import java.time.Instant;

public record SendFormResponse(
        String sentFormId,
        String recipientId,
        int templateVersion,
        Instant expiresAt,
        String messageId) {
}
