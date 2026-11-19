package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record SaveDraftRequest(
        @NotNull @Size(max = 1_000) Map<String, Object> answers,
        @PositiveOrZero Long lockVersion) {
}
