package com.mynx.myca500.patient_forms.repository;


import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.enums.SentFormStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SentFormRepository extends JpaRepository<SentForm, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SentForm s set s.status = :to, s.openedAt = :now "
            + "where s.id = :id and s.status = :from")
    int transition(@Param("id") UUID id,
                   @Param("from") SentFormStatus from,
                   @Param("to") SentFormStatus to,
                   @Param("now") Instant now);

    default int markOpened(UUID id, Instant now) {
        return transition(id, SentFormStatus.SENT, SentFormStatus.OPENED, now);
    }

    interface TemplateSentCount {
        UUID getTemplateId();
        long getTotal();
    }

       @Modifying(clearAutomatically = true, flushAutomatically = true)
   @Query("update SentForm s set s.status = :status, s.submittedAt = :now "
                      + "where s.id = :id and s.submittedAt is null and s.revokedAt is null "
                       + "and (s.expiresAt is null or s.expiresAt > :now)")
   int submitIfOpen(@Param("id") UUID id,
                   @Param("status") SentFormStatus status,
                    @Param("now") Instant now);

            default int markSubmitted(UUID id, Instant now) {
              return submitIfOpen(id, SentFormStatus.SUBMITTED, now);
            }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SentForm s set s.status = :status, "
            + "s.openedAt = coalesce(s.openedAt, :now) "
            + "where s.id = :id and s.submittedAt is null and s.revokedAt is null "
            + "and (s.expiresAt is null or s.expiresAt > :now)")
    int draftIfOpen(@Param("id") UUID id,
                    @Param("status") SentFormStatus status,
                    @Param("now") Instant now);

    default int markInProgress(UUID id, Instant now) {
        return draftIfOpen(id, SentFormStatus.IN_PROGRESS, now);
    }

    Page<SentForm> findByTemplateId(UUID templateId, Pageable pageable);

    Page<SentForm> findByTemplateIdAndPatientId(UUID templateId, String patientId, Pageable pageable);

    @Query("select s.templateId as templateId, count(s) as total from SentForm s "
            + "where s.templateId in :ids group by s.templateId")
    List<TemplateSentCount> countByTemplateIds(@Param("ids") Collection<UUID> ids);
}
