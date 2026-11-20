package com.mynx.myca500.patient_forms.entity;

import com.mynx.myca500.patient_forms.enums.OnboardingStatus;
import com.mynx.myca500.patient_forms.enums.Profession;
import com.mynx.myca500.patient_forms.enums.TeamSize;
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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_user_id", nullable = false, unique = true, updatable = false)
    private UUID keycloakUserId;

    @Column(name = "account_id", nullable = false, unique = true, updatable = false, length = 12)
    private String accountId;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone_e164", length = 16)
    private String phoneE164;

    @Enumerated(EnumType.STRING)
    @Column(name = "profession", length = 40)
    private Profession profession;

    @Column(name = "profession_other", length = 100)
    private String professionOther;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_size", length = 20)
    private TeamSize teamSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 20)
    private OnboardingStatus onboardingStatus;

    @Column(name = "personal_workspace_id", unique = true)
    private UUID personalWorkspaceId;

    @Column(name = "default_workspace_id")
    private UUID defaultWorkspaceId;

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
