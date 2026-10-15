package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InputFormat {
    PLAIN("Plain"), TEXT_AREA("TextArea"), NUMBER("Number"), EMAIL("Email");

    private final String value;

    InputFormat(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}
