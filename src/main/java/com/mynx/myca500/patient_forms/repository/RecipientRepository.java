package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.Recipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, UUID> {

    Optional<Recipient> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Page<Recipient> findByWorkspaceIdAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCaseAndEmailContainingIgnoreCase(
            UUID workspaceId, String firstName, String lastName, String email, Pageable pageable);

    @Query("select r from Recipient r where r.workspaceId = :workspaceId and r.email = :email "
            + "and lower(r.firstName) = lower(:firstName) and lower(r.lastName) = lower(:lastName)")
    Optional<Recipient> findIdentity(@Param("workspaceId") UUID workspaceId,
                                     @Param("email") String email,
                                     @Param("firstName") String firstName,
                                     @Param("lastName") String lastName);
}
