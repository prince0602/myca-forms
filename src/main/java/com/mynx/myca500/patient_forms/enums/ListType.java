package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ListType {
    @JsonProperty("bullet") BULLET,
    @JsonProperty("ordered") ORDERED
}
