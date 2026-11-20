package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findByWorkspaceId(String workspaceId);

    boolean existsByWorkspaceId(String workspaceId);
}
