package com.mynx.myca500.patient_forms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DiagramConfig(
        @Size(max = 2048) String image,
        @Valid @Size(max = 200) List<DiagramPoint> points) {

    public DiagramConfig {
        image = image == null ? "" : image;
        points = points == null ? List.of() : List.copyOf(points);
    }
}
