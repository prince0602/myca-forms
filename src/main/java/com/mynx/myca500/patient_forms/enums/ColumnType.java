package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ColumnType {
    TEXT("Text"), NUMBER("Number"), DATE("Date"), SCALE("Scale"),CHECKBOX("Checkbox");

    private final String value;

    ColumnType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}
