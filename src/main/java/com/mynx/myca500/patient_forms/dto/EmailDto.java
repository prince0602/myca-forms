package com.mynx.myca500.patient_forms.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record EmailDto(
        @NotBlank(message = "Recipient is required")
        @Email(message = "Invalid email address")
        String to,

        @Size(max = 200) String subject,
        @Size(max = 20000) String body,
        String template,
        Map<String, Object> values,
        Long formId,
        Long patientId) {

    public EmailDto {
        to = to == null ? null : to.trim();
        template = (template == null || template.isBlank()) ? null : template.trim();
        values = values == null ? Map.of() : values;
    }

    @JsonIgnore
    @AssertTrue(message = "Provide either a template, or both subject and body")
    public boolean isValidContent() {
        return template != null || (hasText(subject) && hasText(body));
    }

    @JsonIgnore
    @AssertTrue(message = "formId and patientId must be provided together")
    public boolean isValidFormLink() {
        return (formId == null) == (patientId == null);
    }

    public static EmailDto fromTemplate(String to, String template, Map<String, Object> values) {
        return new EmailDto(to, null, null, template, values, null, null);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}