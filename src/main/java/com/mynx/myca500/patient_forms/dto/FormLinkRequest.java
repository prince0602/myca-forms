package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/v1/form-templates/{id}/links.
 * If email is present the link is also emailed to it.
 * If expiresInHours is null, the template version's expirationHours is used, then the
 * configured default (forms.link.default-expiration-hours). Links always expire; the upper
 * bound is forms.template.max-expiration-hours.
 */
public record FormLinkRequest(
        @NotBlank @Size(max = 64) String patientId,
        @Email String email,
        @Positive Integer expiresInHours) {
}
