package com.mynx.myca500.patient_forms.dto;

import java.time.Instant;

/**
 * @param emailMessageId provider message id when the link was emailed, otherwise null
 */
public record FormLinkResponse(
        String sentFormId,
        String templateId,
        int templateVersion,
        String token,
        String link,
        Instant expiresAt,
        String emailMessageId) {
}
