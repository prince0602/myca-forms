package com.mynx.myca500.patient_forms.dto;

import com.mynx.myca500.patient_forms.enums.TextVariant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FormattedText(
        @Size(max = 20000) String text,
        TextVariant variant,
        @Positive @Max(200) Integer fontSize,
        @Pattern(regexp = "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$",
                message = "color must be a hex value like #1a2b3c") String color,
        boolean bold,
        boolean italic,
        boolean underline,
        boolean strikethrough,
        @Pattern(regexp = "left|center|right|justify",
                message = "align must be left, center, right or justify") String align) {

    public FormattedText {
        text = text == null ? "" : text;
        variant = variant == null ? TextVariant.PARAGRAPH : variant;
        align = align == null ? "left" : align;
    }

    public static FormattedText empty() {
        return new FormattedText(null, null, null, null, false, false, false, false, null);
    }
}
