package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.WorkspaceBonus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceBonusRepository extends JpaRepository<WorkspaceBonus, UUID> {

    @Query("""
           select b from WorkspaceBonus b
            where b.workspaceId = :workspaceId
              and b.formsUsed < b.amount
            order by b.grantedAt, b.id
           """)
    List<WorkspaceBonus> findAvailable(@Param("workspaceId") UUID workspaceId, Pageable pageable);

    @Query("select coalesce(sum(b.amount - b.formsUsed), 0) from WorkspaceBonus b where b.workspaceId = :workspaceId")
    long sumRemaining(@Param("workspaceId") UUID workspaceId);
}
