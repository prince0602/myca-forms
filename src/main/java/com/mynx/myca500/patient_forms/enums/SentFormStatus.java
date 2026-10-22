package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SentFormStatus {
    COMPLETE("Complete"), REVOKED("Revoked"), IN_PROGRESS("InProgress");

    private final String value;

    SentFormStatus(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}
