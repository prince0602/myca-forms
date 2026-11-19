package com.mynx.myca500.patient_forms.dto;

import java.time.Instant;

/**
 * One row of "who was this template sent to".
 *
 * @param status effective status: Revoked / Submitted / Expired are derived from the timestamps,
 *               because the stored status is not moved to EXPIRED by any job
 */
public record SentFormListItem(
        String sentFormId,
        String patientId,
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
