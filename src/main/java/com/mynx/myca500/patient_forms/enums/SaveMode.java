package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SaveMode {
    SAVE("Save"), SAVE_AND_ACTIVATE("SaveAndActivate");

    private final String value;

    SaveMode(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }
}
