package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.FormResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface FormResponseRepository extends JpaRepository<FormResponse, UUID> {
    Optional<FormResponse> findBySentFormId(UUID sentFormId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FormResponse r set r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.PROCESSING, "
            + "r.pdfAttempts = r.pdfAttempts + 1, r.pdfProcessingStartedAt = :now, r.pdfNextAttemptAt = null "
            + "where r.id = :id and r.status = 'SUBMITTED' and r.pdfKey is null and ("
            + "  r.pdfStatus is null or r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.PENDING "
            + "  or (r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.FAILED "
            + "      and r.pdfAttempts < :maxAttempts and r.pdfNextAttemptAt <= :now))")
    int claimPdf(@Param("id") UUID id, @Param("now") Instant now, @Param("maxAttempts") int maxAttempts);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FormResponse r set r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.COMPLETED, "
            + "r.pdfKey = :key, r.pdfGeneratedAt = :now, r.pdfProcessingStartedAt = null, "
            + "r.pdfNextAttemptAt = null, r.pdfLastError = null "
            + "where r.id = :id and r.pdfAttempts = :attempt "
            + "and r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.PROCESSING")
    int completePdf(@Param("id") UUID id, @Param("attempt") int attempt,
                    @Param("key") String key, @Param("now") Instant now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FormResponse r set r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.FAILED, "
            + "r.pdfFailedAt = :now, r.pdfLastError = :error, r.pdfNextAttemptAt = :next, "
            + "r.pdfProcessingStartedAt = null "
            + "where r.id = :id and r.pdfAttempts = :attempt "
            + "and r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.PROCESSING")
    int failPdf(@Param("id") UUID id, @Param("attempt") int attempt, @Param("now") Instant now,
                @Param("error") String error, @Param("next") Instant next);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FormResponse r set r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.FAILED, "
            + "r.pdfFailedAt = :now, r.pdfLastError = 'PROCESSING_TIMEOUT', r.pdfNextAttemptAt = :now, "
            + "r.pdfProcessingStartedAt = null "
            + "where r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.PROCESSING "
            + "and r.pdfProcessingStartedAt < :staleBefore")
    int expireStalePdfJobs(@Param("staleBefore") Instant staleBefore, @Param("now") Instant now);


    @Query("select r.id from FormResponse r where r.status = 'SUBMITTED' and r.pdfKey is null and ("
            + "  (r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.PENDING and r.submittedAt < :pendingBefore) "
            + "  or (r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.FAILED "
            + "      and r.pdfAttempts < :maxAttempts and r.pdfNextAttemptAt <= :now)) "
            + "order by r.submittedAt")
    List<UUID> findDuePdfJobs(@Param("now") Instant now, @Param("pendingBefore") Instant pendingBefore,
                              @Param("maxAttempts") int maxAttempts, Pageable page);
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FormResponse r set r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.PENDING, "
            + "r.pdfAttempts = 0, r.pdfNextAttemptAt = null "
            + "where r.id = :id and r.status = 'SUBMITTED' and r.pdfKey is null "
            + "and r.pdfStatus = com.mynx.myca500.patient_forms.enums.PdfStatus.FAILED")
    int resetFailedPdf(@Param("id") UUID id);
}
