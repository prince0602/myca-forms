package com.mynx.myca500.patient_forms.dto;


import com.mynx.myca500.patient_forms.enums.SaveMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


public record SaveFormTemplateRequest(
        @NotNull SaveMode mode,
        @Positive Integer baseVersion,
        @NotNull @Valid FormTemplatePayload payload) {
}
