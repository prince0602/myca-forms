package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.BaaDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BaaDocumentRepository extends JpaRepository<BaaDocument, String> {

    Optional<BaaDocument> findFirstByCurrentTrue();
}
