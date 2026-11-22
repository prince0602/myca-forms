package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.FormLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormLinkRepository extends JpaRepository<FormLink, UUID> {
    Optional<FormLink> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FormLink l set l.usedAt = :now, l.lastAccessedAt = :now "
            + "where l.id = :id and l.usedAt is null")
    int markUsed(@Param("id") UUID id, @Param("now") Instant now);

}
