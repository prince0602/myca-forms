package com.mynx.myca500.patient_forms.dto;


import com.mynx.myca500.patient_forms.enums.SaveMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body for POST (create) and PUT (new version).
 * {@code baseVersion} = the version the editor loaded. Required on PUT, ignored on POST.
 * It is how we detect "someone else saved first" (AC3-S12).
 */
public record SaveFormTemplateRequest(
        @NotNull SaveMode mode,
        @Positive Integer baseVersion,
        @NotNull @Valid FormTemplatePayload payload) {
}
