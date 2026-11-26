package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchUploadRequest(
        @NotEmpty @Size(max = BatchUploadRequest.MAX_FILES) List<@NotBlank String> extensions) {

    public static final int MAX_FILES = 25;
}
