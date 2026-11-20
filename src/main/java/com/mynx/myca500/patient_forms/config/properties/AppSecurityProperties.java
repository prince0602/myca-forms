package com.mynx.myca500.patient_forms.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app.security")
public record AppSecurityProperties(String audience, List<String> allowedOrigins) {
}
