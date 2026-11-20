package com.mynx.myca500.patient_forms.enums;

import lombok.Getter;

@Getter
public enum TeamSize {
    JUST_ME("Just me"),
    SIZE_2_10("2–10"),
    SIZE_11_50("11–50"),
    SIZE_51_200("51–200"),
    SIZE_201_PLUS("201+");

    private final String label;

    TeamSize(String label) {
        this.label = label;
    }

}
