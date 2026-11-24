package com.mynx.myca500.patient_forms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Body for POST /api/workspaces/{workspaceId}/recipients/send-form.
 * If expiresInHours is null, the template version's expirationHours is used, then the server default.
 */
public record SendFormToRecipientRequest(
        @NotNull UUID templateId,
        @Positive Integer expiresInHours,
        @NotNull @Valid RecipientDetails recipient) {
}
