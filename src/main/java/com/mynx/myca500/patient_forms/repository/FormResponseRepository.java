package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.FormResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FormResponseRepository extends JpaRepository<FormResponse, UUID> {
    Optional<FormResponse> findBySentFormId(UUID sentFormId);
}
