package com.mynx.myca500.patient_forms.validation;


import com.mynx.myca500.patient_forms.dto.FieldOption;
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

        Set<String> tabNames = new HashSet<>();
        Set<String> ids = new HashSet<>();
        List<FormFieldDto> keptTabs = new ArrayList<>();

        for (FormFieldDto tab : payload.fields()) {
//            if (tab.type() != FieldType.TAB) {
//                errors.add("Only tabs are allowed at the top level (found " + describe(tab) + ")");
//                continue;
//            }
            if (!ids.add(tab.id())) {
                errors.add("Duplicate id: " + tab.id());
            }
//            if (tab.label().isBlank()) {
//                errors.add("Every tab must have a name");
//            } else if (!tabNames.add(tab.label().trim().toLowerCase(Locale.ROOT))) {
//                errors.add("Tab name must be unique within the template: " + tab.label());
//            }
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
//        if (!f.children().isEmpty()) {
//            errors.add(describe(f) + " cannot have children");
//        }
//        if (f.type() == FieldType.FREE_TEXT) {
//            if (f.config().content().text().isBlank()) {
//                errors.add("Free text content is required");
//            }
//            return;
//        }
        if (f.type().isChoice()) {
            validateOptions(f, errors);
        }
    }

    private void validateOptions(FormFieldDto f, List<String> errors) {
        List<FieldOption> options = f.config().options();
        int min = f.type().minOptions();
//        if (options.size() < min) {
//            errors.add(describe(f) + " needs at least " + min + " option(s)");
//        }
        Set<String> labels = new HashSet<>();
        Set<String> optionIds = new HashSet<>();
//        for (FieldOption o : options) {
//            if (!labels.add(o.label().trim().toLowerCase(Locale.ROOT))) {
//                errors.add("Duplicate option '" + o.label() + "' in " + describe(f));
//            }
//            if (!optionIds.add(o.id())) {
//                errors.add("Duplicate option id '" + o.id() + "' in " + describe(f));
//            }
//        }
    }

//    private static String describe(FormFieldDto f) {
//        return "'" + (f.label().isBlank() ? f.id() : f.label()) + "'";
//    }
}
