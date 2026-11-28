package com.mynx.myca500.patient_forms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;


@ConfigurationProperties("forms.pdf")
public record PdfProperties(
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("PT1M") Duration retryBackoff,
        @DefaultValue("PT1H") Duration maxRetryBackoff,
        @DefaultValue("PT10M") Duration processingTimeout,
        @DefaultValue("PT1M") Duration sweepInterval,
        @DefaultValue("PT2M") Duration pendingGrace,
        @DefaultValue("20") int sweepBatchSize,
        @DefaultValue("10485760") long maxAttachmentBytes,
        @DefaultValue("50") int maxAttachmentPages,
        @DefaultValue("200") int maxAppendedPages,
        @DefaultValue("25000000") long maxDiagramPixels) {

    public static PdfProperties defaults() {
        return new PdfProperties(5, Duration.ofMinutes(1), Duration.ofHours(1), Duration.ofMinutes(10),
                Duration.ofMinutes(1), Duration.ofMinutes(2), 20, 10_485_760, 50, 200, 25_000_000);
    }
}
