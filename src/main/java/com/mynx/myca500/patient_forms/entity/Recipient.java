package com.mynx.myca500.patient_forms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * A person a form is sent to, scoped to a workspace. Created from the "send form" prompt.
 * Identity within a workspace is email + first name + last name (names case-insensitive),
 * so one email can belong to several recipients with different names (ux_recipients_identity).
 */
@Entity
@Table(name = "recipients")
@Getter
@Setter
@NoArgsConstructor
public class Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Lower-cased by EmailUtil.normalize. */
    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "phone_number", length = 25)
    private String phoneNumber;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "dob")
    private LocalDate dob;

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

    public String fullName() {
        return (firstName + " " + lastName).trim();
    }
}
