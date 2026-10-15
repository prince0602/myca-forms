package com.mynx.myca500.patient_forms.repository;


import com.mynx.myca500.patient_forms.entity.FormTemplate;
import com.mynx.myca500.patient_forms.enums.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FormTemplateRepository extends JpaRepository<FormTemplate, UUID> {

    boolean existsByStatusAndNameIgnoreCase(TemplateStatus status, String name);

    boolean existsByStatusAndNameIgnoreCaseAndIdNot(TemplateStatus status, String name, UUID id);
}
