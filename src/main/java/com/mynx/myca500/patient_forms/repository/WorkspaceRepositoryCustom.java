package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.Workspace;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepositoryCustom {

    Optional<Workspace> lockById(UUID id);
}
