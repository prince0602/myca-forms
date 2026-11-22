package com.mynx.myca500.patient_forms.dto;

import java.util.List;
import java.util.Map;

/**
 * Staff preview of a sent form: the exact template version the patient received, plus their
 * answers so far.
 *
 * @param responseStatus null (nothing saved yet), DRAFT (autosaved, not submitted) or SUBMITTED
 */
public record SentFormPreview(
        SentFormListItem sentForm,
        String title,
        String description,
        List<FormFieldDto> fields,
        Map<String, Object> answers,
        String responseStatus) {
}
