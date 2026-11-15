package com.mynx.myca500.patient_forms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "form_links")
@Data
public class FormLink {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false, unique = true) private String tokenHash;
    private Long formId;
    private Long patientId;
    private Instant expiresAt;
    private Instant usedAt;

}


