package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TextVariant {
    PARAGRAPH("paragraph"), H1("h1"), H2("h2"), H3("h3"), H4("h4"), H5("h5");

    private final String value;

    TextVariant(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}
