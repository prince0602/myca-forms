package com.mynx.myca500.patient_forms.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class BillingCycleUtil {

    private static final long MINUTES_PER_DAY = Duration.ofDays(1).toMinutes();

    private BillingCycleUtil() {
    }


    public static Instant nextRefreshAfter(Instant anchor, Instant now) {
        ZonedDateTime start = anchor.atZone(ZoneOffset.UTC);
        ZonedDateTime current = now.atZone(ZoneOffset.UTC);
        long months = Math.max(ChronoUnit.MONTHS.between(start, current), 0);
        ZonedDateTime candidate = start.plusMonths(months);
        while (!candidate.isAfter(current)) {
            candidate = start.plusMonths(++months);
        }
        return candidate.toInstant();
    }

    public static boolean isRefreshDue(Instant usageRefreshAt, Instant now) {
        return usageRefreshAt == null || !usageRefreshAt.isAfter(now);
    }
    public static long daysUntil(Instant refreshAt, Instant now) {
        long minutes = Duration.between(now, refreshAt).toMinutes();
        return minutes <= 0 ? 0 : (minutes + MINUTES_PER_DAY - 1) / MINUTES_PER_DAY;
    }
}
