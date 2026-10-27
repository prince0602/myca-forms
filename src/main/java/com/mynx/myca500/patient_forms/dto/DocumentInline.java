package com.mynx.myca500.patient_forms.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(DocumentText.class),
        @JsonSubTypes.Type(DocumentFieldRef.class)
})
public sealed interface DocumentInline permits DocumentText, DocumentFieldRef {}
