package com.mynx.myca500.patient_forms.entity;

import com.mynx.myca500.patient_forms.enums.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "plan_limits")
@Getter
@Setter
@NoArgsConstructor
public class PlanLimit {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, updatable = false, length = 20)
    private Plan plan;

    @Column(name = "monthly_limit", nullable = false)
    private int monthlyLimit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
