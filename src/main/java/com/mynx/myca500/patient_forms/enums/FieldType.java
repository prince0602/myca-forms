package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FieldType {
    TAB("Tab"), FREE_TEXT("FreeText"), INPUT("Input"), DROPDOWN("Dropdown"),
    CHECKBOX("Checkbox"), RADIO("Radio"), DATE("Date"), SIGNATURE("Signature");

    private final String value;

    FieldType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    public boolean isChoice() { return this == DROPDOWN || this == CHECKBOX || this == RADIO; }

    /** AC4-S6: Dropdown/Radio need 2+ options, Checkbox needs 1+. */
    public int minOptions() { return this == CHECKBOX ? 1 : 2; }
}
