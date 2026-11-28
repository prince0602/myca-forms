package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.Workspace;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;
import java.util.UUID;

class WorkspaceRepositoryCustomImpl implements WorkspaceRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Workspace> lockById(UUID id) {
        Workspace workspace = entityManager.find(Workspace.class, id);
        if (workspace == null) {
            return Optional.empty();
        }
        entityManager.refresh(workspace, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(workspace);
    }
}
