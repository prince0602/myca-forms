package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.SavedComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavedComponentRepository extends JpaRepository<SavedComponent, UUID> {

    boolean existsByNameIgnoreCase(String name);

    List<SavedComponent> findAllByOrderByNameAsc();
}