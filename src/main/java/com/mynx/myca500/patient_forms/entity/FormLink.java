package com.mynx.myca500.patient_forms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity @Table(name = "form_links")
public class FormLink {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true) private String tokenHash;
    @Column(nullable = false) private UUID sentFormId;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant revokedAt;
    private Instant lastAccessedAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}


