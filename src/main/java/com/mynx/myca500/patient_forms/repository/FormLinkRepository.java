package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.FormLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormLinkRepository extends JpaRepository<FormLink, UUID> {
    Optional<FormLink> findByTokenHash(String tokenHash);
}
