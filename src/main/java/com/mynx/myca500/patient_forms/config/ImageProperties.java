package com.mynx.myca500.patient_forms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("forms.image")
public record ImageProperties(
        @DefaultValue("5242880") long maxBytes,
        @DefaultValue("PT60M") Duration presignTtl,
        @DefaultValue("10000") long cacheMaxEntries) {}