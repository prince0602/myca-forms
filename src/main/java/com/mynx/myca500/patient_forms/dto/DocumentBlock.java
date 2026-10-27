package com.mynx.myca500.patient_forms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mynx.myca500.patient_forms.enums.BlockTag;
import com.mynx.myca500.patient_forms.enums.ListType;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentBlock(
        BlockTag tag,
        ListType list,
        String className,
        Map<String, Object> style,
        List<DocumentInline> children
) {}