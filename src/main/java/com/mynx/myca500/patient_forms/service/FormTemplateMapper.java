package com.mynx.myca500.patient_forms.service;


import com.mynx.myca500.patient_forms.dto.FormTemplateDetailResponse;
import com.mynx.myca500.patient_forms.dto.FormTemplateListItem;
import com.mynx.myca500.patient_forms.entity.FormTemplate;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import org.springframework.stereotype.Component;

@Component
class FormTemplateMapper {

    FormTemplateDetailResponse toDetail(FormTemplate t, FormTemplateVersion v) {
        return new FormTemplateDetailResponse(
                t.getId().toString(), v.getLabel(), v.getDescription(), t.getStatus(), v.getVersionNumber(),
                t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedBy(), t.getUpdatedAt(),
                v.getExpirationHours(), v.getFields());
    }

    FormTemplateListItem toListItem(FormTemplate t) {
        return new FormTemplateListItem(
                t.getId().toString(), t.getName(), t.getDescription(), t.getStatus(), t.getCurrentVersion(),
                t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedBy(), t.getUpdatedAt(), 0L);
    }
}
