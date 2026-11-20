package com.mynx.myca500.patient_forms.entity;

import com.mynx.myca500.patient_forms.enums.AuditAction;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 60)
    private AuditAction action;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "workspace_id", updatable = false)
    private UUID workspaceId;

    @Column(name = "target", updatable = false, length = 100)
    private String target;

    @Column(name = "ip", updatable = false, length = 45)
    private String ip;

    @Column(name = "user_agent", updatable = false, length = 512)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> details;
}
