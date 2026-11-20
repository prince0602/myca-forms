package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
