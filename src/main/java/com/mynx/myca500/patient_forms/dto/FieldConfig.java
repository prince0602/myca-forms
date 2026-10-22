package com.mynx.myca500.patient_forms.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mynx.myca500.patient_forms.enums.InputFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FieldConfig(
        @Size(max = 255) String placeholder,
        @Size(max = 1000) String prefix,
        @Size(max = 1000) String suffix,
        @Size(max = 2000) String helpText,
        @Positive Integer maxLength,
        InputFormat format,
        @Valid List<FieldOption> options,
        @Valid FormattedText content,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Valid @Size(max = 50) List<TableColumn> columns,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Valid DiagramConfig diagram,
        @JsonInclude(JsonInclude.Include.NON_NULL) Double min,
        @JsonInclude(JsonInclude.Include.NON_NULL) Double max,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Positive Double width,
        @Valid VisibilityRule visibleWhen) {

    public FieldConfig {
        placeholder = nz(placeholder);
        prefix = nz(prefix);
        suffix = nz(suffix);
        helpText = nz(helpText);
        format = format == null ? InputFormat.PLAIN : format;
        options = options == null ? List.of() : List.copyOf(options);
        content = content == null ? FormattedText.empty() : content;
        columns = columns == null ? null : List.copyOf(columns);
    }

    public static FieldConfig empty() {
        return new FieldConfig(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    private static String nz(String s) { return s == null ? "" : s; }
}

