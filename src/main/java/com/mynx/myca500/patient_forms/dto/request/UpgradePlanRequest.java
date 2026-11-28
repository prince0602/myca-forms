package com.mynx.myca500.patient_forms.dto.request;

import com.mynx.myca500.patient_forms.enums.BillingInterval;
import com.mynx.myca500.patient_forms.enums.Plan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpgradePlanRequest(
        @NotNull Plan plan,
        @NotNull BillingInterval billingInterval,
        @NotBlank @Size(max = 100) String paymentReference) {
}
