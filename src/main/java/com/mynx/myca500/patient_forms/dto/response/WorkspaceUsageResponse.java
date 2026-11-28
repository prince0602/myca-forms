package com.mynx.myca500.patient_forms.dto.response;

import com.mynx.myca500.patient_forms.enums.BillingInterval;
import com.mynx.myca500.patient_forms.enums.Plan;
import com.mynx.myca500.patient_forms.model.PlanPrice;
import com.mynx.myca500.patient_forms.model.UsageSnapshot;
import com.mynx.myca500.patient_forms.util.BillingCycleUtil;

import java.time.Instant;
import java.util.List;

public record WorkspaceUsageResponse(
        Plan plan,
        BillingInterval billingInterval,
        int formsUsed,
        int monthlyLimit,
        int monthlyFormsRemaining,
        boolean customLimit,
        long bonusFormsRemaining,
        boolean sendingBlocked,
        Instant refreshesAt,
        long daysUntilRefresh,
        boolean canInvite,
        int maxUsers,
        UpgradeOffer upgradeOffer) {

    public record UpgradeOffer(Plan plan, BillingInterval defaultInterval, int monthlyLimit, int maxUsers,
                               List<PriceOption> prices) {
    }

    public record PriceOption(BillingInterval interval, long monthlyEquivalentCents, long chargeCents,
                              String currency, String savingsLabel) {
    }

    public static WorkspaceUsageResponse from(UsageSnapshot usage, int plusMonthlyLimit, Instant now) {
        return new WorkspaceUsageResponse(usage.plan(), usage.billingInterval(), usage.formsUsed(),
                usage.monthlyLimit(), usage.monthlyFormsRemaining(), usage.customLimit(),
                usage.bonusFormsRemaining(), usage.sendingBlocked(), usage.refreshAt(),
                BillingCycleUtil.daysUntil(usage.refreshAt(), now), usage.plan().allowsInvites(),
                usage.plan().maxUsers(), usage.plan() == Plan.FREE ? plusOffer(plusMonthlyLimit) : null);
    }

    private static UpgradeOffer plusOffer(int plusMonthlyLimit) {
        long savingsCents = PlanPrice.PLUS_MONTHLY.monthlyEquivalentCents() - PlanPrice.PLUS_ANNUAL.monthlyEquivalentCents();
        List<PriceOption> prices = List.of(
                option(PlanPrice.PLUS_ANNUAL, "save $" + savingsCents / 100 + "/mo"),
                option(PlanPrice.PLUS_MONTHLY, null));
        return new UpgradeOffer(Plan.PLUS, BillingInterval.ANNUAL, plusMonthlyLimit, Plan.PLUS.maxUsers(), prices);
    }

    private static PriceOption option(PlanPrice price, String savingsLabel) {
        return new PriceOption(price.interval(), price.monthlyEquivalentCents(), price.chargeCents(),
                price.currency(), savingsLabel);
    }
}
