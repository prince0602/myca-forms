package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FormattedText(
        @Size(max = 20000) String text,
        boolean bold,
        boolean italic,
        @Pattern(regexp = "left|center|right|justify", message = "align must be left, center, right or justify") String align) {

    public FormattedText {
        text = text == null ? "" : text;
        align = align == null ? "left" : align;
    }
}
