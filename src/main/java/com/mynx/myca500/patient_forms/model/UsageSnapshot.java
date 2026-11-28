package com.mynx.myca500.patient_forms.model;

import com.mynx.myca500.patient_forms.enums.BillingInterval;
import com.mynx.myca500.patient_forms.enums.Plan;

import java.time.Instant;

public record UsageSnapshot(Plan plan, BillingInterval billingInterval, int monthlyLimit, boolean customLimit,
                            int formsUsed, long bonusFormsRemaining, Instant refreshAt) {

    public int monthlyFormsRemaining() {
        return Math.max(monthlyLimit - formsUsed, 0);
    }

    public boolean sendingBlocked() {
        return monthlyFormsRemaining() == 0 && bonusFormsRemaining == 0;
    }
}
