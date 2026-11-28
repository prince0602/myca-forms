package com.mynx.myca500.patient_forms.entity;

import com.mynx.myca500.patient_forms.enums.BillingInterval;
import com.mynx.myca500.patient_forms.enums.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_payments")
@Getter
@Setter
@NoArgsConstructor
public class WorkspacePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "payment_reference", nullable = false, updatable = false, length = 100)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, updatable = false, length = 20)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, updatable = false, length = 10)
    private BillingInterval billingInterval;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private long amountCents;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "paid_by", nullable = false, updatable = false)
    private UUID paidBy;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;
}
