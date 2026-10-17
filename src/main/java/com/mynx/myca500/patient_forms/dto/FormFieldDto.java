package com.mynx.myca500.patient_forms.dto;


import com.mynx.myca500.patient_forms.enums.FieldType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public record FormFieldDto(
        @Size(max = 100) String id,
        @Size(max = 100) String name,
        @NotNull FieldType type,
        @Size(max = 500) String label,
        boolean required,
        @Valid FieldConfig config,
        Map<String, Object> style,
        @Size(max = 200) String className,
        @Valid List<FormFieldDto> children) {

    public FormFieldDto {
        id = isBlank(id) ? "fld-" + UUID.randomUUID() : id;
        name = isBlank(name) ? "grp-" + UUID.randomUUID() : name;
        label = label == null ? "" : label;
        config = config == null ? FieldConfig.empty() : config;
        style = style == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(style));
        className = className == null ? "" : className;
        children = children == null ? List.of() : List.copyOf(children);
    }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
