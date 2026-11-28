package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.PlanLimit;
import com.mynx.myca500.patient_forms.enums.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanLimitRepository extends JpaRepository<PlanLimit, Plan> {
}
