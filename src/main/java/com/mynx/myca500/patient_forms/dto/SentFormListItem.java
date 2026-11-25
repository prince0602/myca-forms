package com.mynx.myca500.patient_forms.dto;

import java.time.Instant;


public record SentFormListItem(
        String sentFormId,
        String recipientId,
        String recipientName,
        String recipientEmail,
        int templateVersion,
        String status,
        Instant sentAt,
        Instant openedAt,
        Instant submittedAt,
        Instant expiresAt,
        Instant revokedAt,
        String sentBy) {
}
