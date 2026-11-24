package com.mynx.myca500.patient_forms.entity;

import com.mynx.myca500.patient_forms.enums.SentFormStatus;
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
@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "sent_forms")
public class SentForm {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private UUID templateId;
    @Column(nullable = false) private int templateVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SentFormStatus status;

    @Column(nullable = false) private Instant sentAt;

    @Column(nullable = false, length = 64) private String recipientId;
    @Column(length = 320) private String recipientEmail;
    private Instant expiresAt;
    private Instant openedAt;
    private Instant submittedAt;
    private Instant revokedAt;
    private String createdBy;
}
