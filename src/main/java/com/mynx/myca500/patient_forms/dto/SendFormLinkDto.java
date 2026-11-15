package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendFormLinkDto(
        @NotBlank @Email String email,
        @NotNull Long formId,
        @NotNull Long patientId,
        @Min(1) @Max(168) Integer expiresInHours) {}