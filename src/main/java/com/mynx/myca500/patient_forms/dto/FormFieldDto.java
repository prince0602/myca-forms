package com.mynx.myca500.patient_forms.dto;


import com.mynx.myca500.patient_forms.enums.FieldType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public record FormFieldDto(
        @NotBlank @Size(max = 100) String id,
        @NotBlank @Size(max = 100) String name,
        @NotNull FieldType type,
        @Size(max = 500) String label,
        boolean required,
        @Valid FieldConfig config,
        Map<String, Object> style,
        @Size(max = 200) String className,
        @Valid List<FormFieldDto> children) {

    public FormFieldDto {
        label = label == null ? "" : label;
        config = config == null ? FieldConfig.empty() : config;
        style = style == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(style));
        className = className == null ? "" : className;
        children = children == null ? List.of() : List.copyOf(children);
    }
}
