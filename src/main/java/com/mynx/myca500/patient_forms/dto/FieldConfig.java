package com.mynx.myca500.patient_forms.dto;


import com.mynx.myca500.patient_forms.enums.InputFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FieldConfig(
        @Size(max = 255) String placeholder,
        @Size(max = 50) String prefix,
        @Size(max = 50) String suffix,
        @Size(max = 500) String helpText,
        @Positive Integer maxLength,
        InputFormat format,
        @Valid List<FieldOption> options,
        @Valid FormattedText content) {

    public FieldConfig {
        placeholder = nz(placeholder);
        prefix = nz(prefix);
        suffix = nz(suffix);
        helpText = nz(helpText);
        format = format == null ? InputFormat.PLAIN : format;
        options = options == null ? List.of() : List.copyOf(options);
        content = content == null ? new FormattedText(null, false, false, null) : content;
    }

    public static FieldConfig empty() {
        return new FieldConfig(null, null, null, null, null, null, null, null);
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
