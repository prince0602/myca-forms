package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;


public record SendFormLinkDto(
        @Email String email,
        @NotNull UUID templateId,
        @NotBlank @Size(max = 64) String patientId,
        @Positive Integer expiresInHours) {}
