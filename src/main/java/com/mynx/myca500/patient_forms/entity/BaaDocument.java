package com.mynx.myca500.patient_forms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "baa_documents")
@Getter
@Setter
@NoArgsConstructor
public class BaaDocument {

    @Id
    @Column(name = "version", length = 20)
    private String version;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "content_url", nullable = false, length = 500)
    private String contentUrl;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "is_current", nullable = false)
    private boolean current;
}
