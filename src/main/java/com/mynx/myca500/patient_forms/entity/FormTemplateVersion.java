package com.mynx.myca500.patient_forms.entity;

import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Immutable
@Entity
@Table(name = "form_template_versions")
@Data
public class FormTemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 300)
    private String label;

    @Column(length = 2000)
    private String description;

    private Integer expirationHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name= "form_fields", columnDefinition = "jsonb",nullable = false)
    private List<FormFieldDto> fields;

    @Column(updatable = false)
    private String createdBy;

    @Column(updatable = false, name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
