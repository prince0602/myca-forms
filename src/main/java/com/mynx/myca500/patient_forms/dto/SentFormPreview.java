package com.mynx.myca500.patient_forms.dto;

import java.util.List;
import java.util.Map;


public record SentFormPreview(
        SentFormListItem sentForm,
        String title,
        String description,
        List<FormFieldDto> fields,
        Map<String, Object> answers,
        String responseStatus,
        Map<String, String> fileUrls,
        String pdfUrl,
        String pdfStatus) {
}
