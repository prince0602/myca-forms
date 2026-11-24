package com.mynx.myca500.patient_forms.dto.response;

import java.time.Instant;

/** Result of POST /api/workspaces/{workspaceId}/recipients/send-form. messageId is the email provider's id. */
public record SendFormResponse(
        String sentFormId,
        String recipientId,
        int templateVersion,
        Instant expiresAt,
        String messageId) {
}
