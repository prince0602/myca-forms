package com.mynx.myca500.patient_forms.repository;


import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.enums.SentFormStatus;
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

    @Query("select s.templateId as templateId, count(s) as total from SentForm s "
            + "where s.templateId in :ids group by s.templateId")
    List<TemplateSentCount> countByTemplateIds(@Param("ids") Collection<UUID> ids);
}
