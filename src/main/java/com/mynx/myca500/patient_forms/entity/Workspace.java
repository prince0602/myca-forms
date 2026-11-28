package com.mynx.myca500.patient_forms.entity;

import com.mynx.myca500.patient_forms.enums.AccountType;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, unique = true, updatable = false, length = 6)
    private String workspaceId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private Plan plan;

    @Column(name = "custom_monthly_limit")
    private Integer customMonthlyLimit;

    @Column(name = "forms_used", nullable = false)
    private int formsUsed;

    @Column(name = "usage_refresh_at")
    private Instant usageRefreshAt;

    @Column(name = "billing_anchor_at", nullable = false)
    private Instant billingAnchorAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", length = 10)
    private BillingInterval billingInterval;

    @Column(name = "baa_version", length = 20)
    private String baaVersion;

    @Column(name = "baa_accepted_by")
    private UUID baaAcceptedBy;

    @Column(name = "baa_accepted_at")
    private Instant baaAcceptedAt;

    @Column(name = "baa_accepted_ip", length = 45)
    private String baaAcceptedIp;

    @Column(name = "baa_accepted_user_agent", length = 512)
    private String baaAcceptedUserAgent;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;
}
