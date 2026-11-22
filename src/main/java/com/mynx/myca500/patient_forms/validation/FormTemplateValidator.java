package com.mynx.myca500.patient_forms.validation;


import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.dto.FormTemplatePayload;
import com.mynx.myca500.patient_forms.enums.FieldType;
import com.mynx.myca500.patient_forms.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
public class FormTemplateValidator {

    private final int maxExpirationHours;

    public FormTemplateValidator(@Value("${forms.template.max-expiration-hours:8760}") int maxExpirationHours) {
        this.maxExpirationHours = maxExpirationHours;
    }

    public List<FormFieldDto> validateAndNormalize(FormTemplatePayload payload) {
        List<String> errors = new ArrayList<>();

        Integer exp = payload.expirationHours();
        if (exp != null && (exp < 1 || exp > maxExpirationHours)) {
            errors.add("Expiration must be a whole number of hours between 1 and " + maxExpirationHours);
        }

        Set<String> ids = new HashSet<>();
        List<FormFieldDto> keptTabs = new ArrayList<>();

        for (FormFieldDto tab : payload.fields()) {
            if (!ids.add(tab.id())) {
                errors.add("Duplicate id: " + tab.id());
            }
            if (tab.children().isEmpty()) {
                continue;
            }
            tab.children().forEach(f -> validateField(f, errors, ids));
            keptTabs.add(tab);
        }

        if (errors.isEmpty() && keptTabs.isEmpty()) {
            errors.add("Add at least one field to a tab");
        }
        if (!errors.isEmpty()) {
            throw ApiException.badRequest(errors);
        }
        return keptTabs;
    }

    private void validateField(FormFieldDto f, List<String> errors, Set<String> ids) {
        if (!ids.add(f.id())) {
            errors.add("Duplicate id: " + f.id());
        }
        if (f.type() == FieldType.TAB) {
            errors.add("Tabs cannot be nested inside a tab");
            return;
        }
        if (f.type().isChoice()) {
            validateOptions(f, errors);
        }
    }

    private void validateOptions(FormFieldDto f, List<String> errors) {

    }
}
