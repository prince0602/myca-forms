package com.mynx.myca500.patient_forms.model;

import com.mynx.myca500.patient_forms.enums.BillingInterval;
import com.mynx.myca500.patient_forms.enums.Plan;

import java.util.List;
import java.util.Optional;

public record PlanPrice(Plan plan, BillingInterval interval, long monthlyEquivalentCents, long chargeCents,
                        String currency) {

    public static final String USD = "USD";

    public static final PlanPrice PLUS_ANNUAL = new PlanPrice(Plan.PLUS, BillingInterval.ANNUAL, 9_900, 118_800, USD);
    public static final PlanPrice PLUS_MONTHLY = new PlanPrice(Plan.PLUS, BillingInterval.MONTHLY, 12_000, 12_000, USD);

    public static final List<PlanPrice> CATALOG = List.of(PLUS_ANNUAL, PLUS_MONTHLY);

    public static Optional<PlanPrice> of(Plan plan, BillingInterval interval) {
        return CATALOG.stream().filter(p -> p.plan() == plan && p.interval() == interval).findFirst();
    }
}
