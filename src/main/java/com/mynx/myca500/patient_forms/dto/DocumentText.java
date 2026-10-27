package com.mynx.myca500.patient_forms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentText(String text, Map<String, Object> style) implements DocumentInline {}
