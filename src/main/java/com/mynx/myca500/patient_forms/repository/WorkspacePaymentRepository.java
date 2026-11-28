package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.WorkspacePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspacePaymentRepository extends JpaRepository<WorkspacePayment, UUID> {

    Optional<WorkspacePayment> findByPaymentReference(String paymentReference);
}
