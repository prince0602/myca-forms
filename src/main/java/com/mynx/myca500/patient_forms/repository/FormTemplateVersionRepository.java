package com.mynx.myca500.patient_forms.repository;


import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FormTemplateVersionRepository extends JpaRepository<FormTemplateVersion, UUID> {

    Optional<FormTemplateVersion> findByTemplateIdAndVersionNumber(UUID templateId, int versionNumber);
}
