package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TemplateStatus {
    ACTIVE("Active"), INACTIVE("Inactive");

    private final String value;

    TemplateStatus(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}
