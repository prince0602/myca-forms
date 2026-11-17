package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SentFormStatus {

    SENT("Sent"),
    OPENED("Opened"),
    IN_PROGRESS("InProgress"),
    SUBMITTED("Submitted"),
    EXPIRED("Expired"),
    REVOKED("Revoked");

    private final String value;

    SentFormStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
