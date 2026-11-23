package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.Invitation;
import com.mynx.myca500.patient_forms.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<Invitation> findByWorkspaceIdAndEmailAndStatus(UUID workspaceId, String email, InvitationStatus status);

    List<Invitation> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, InvitationStatus status);

    long countByWorkspaceIdAndEmailAndCreatedAtAfter(UUID workspaceId, String email, Instant since);
}
