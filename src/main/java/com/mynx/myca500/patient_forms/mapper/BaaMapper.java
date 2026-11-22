package com.mynx.myca500.patient_forms.mapper;

import com.mynx.myca500.patient_forms.dto.response.BaaResponse;
import com.mynx.myca500.patient_forms.entity.BaaDocument;

public final class BaaMapper {

    private BaaMapper() {
    }

    public static BaaResponse toResponse(BaaDocument document) {
        return new BaaResponse(document.getVersion(), document.getContentUrl(), document.getContentHash());
    }
}
