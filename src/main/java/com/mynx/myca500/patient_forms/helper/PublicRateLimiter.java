package com.mynx.myca500.patient_forms.helper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;


@Component
public class PublicRateLimiter {

    private final int maxRequestsPerMinute;
    private final int maxFailuresPerMinute;

    private final Cache<String, AtomicInteger> requests = newWindow();
    private final Cache<String, AtomicInteger> failures = newWindow();

    public PublicRateLimiter(
            @Value("${app.public.rate-limit-per-minute:60}") int maxRequestsPerMinute,
            @Value("${app.public.failure-limit-per-minute:10}") int maxFailuresPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxFailuresPerMinute = maxFailuresPerMinute;
    }


    public boolean allowRequest(String ip) {
        return requests.get(ip, k -> new AtomicInteger()).incrementAndGet() <= maxRequestsPerMinute;
    }

    public void recordFailure(String ip) {
        failures.get(ip, k -> new AtomicInteger()).incrementAndGet();
    }

    public boolean tooManyFailures(String ip) {
        AtomicInteger count = failures.getIfPresent(ip);
        return count != null && count.get() >= maxFailuresPerMinute;
    }

    private static Cache<String, AtomicInteger> newWindow() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(100_000)
                .build();
    }
}
