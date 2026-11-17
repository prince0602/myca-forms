package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * The template version is not accepted from the client: the link is always pinned to the
 * template's current version on the server.
 * If expiresInHours is null, the version's expirationHours is used, then the configured default.
 * The upper bound is checked in the service against forms.template.max-expiration-hours.
 */
public record SendFormLinkDto(
        @NotBlank @Email String email,
        @NotNull UUID templateId,
        @NotBlank @Size(max = 64) String patientId,
        @Positive Integer expiresInHours) {}
