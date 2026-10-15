package com.mynx.myca500.patient_forms.repository;


import com.mynx.myca500.patient_forms.entity.SentForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SentFormRepository extends JpaRepository<SentForm, UUID> {

    interface TemplateSentCount {
        UUID getTemplateId();
        long getTotal();
    }

    /** One grouped query for the whole page (avoids N+1). */
    @Query("select s.templateId as templateId, count(s) as total from SentForm s "
            + "where s.templateId in :ids group by s.templateId")
    List<TemplateSentCount> countByTemplateIds(@Param("ids") Collection<UUID> ids);
}
