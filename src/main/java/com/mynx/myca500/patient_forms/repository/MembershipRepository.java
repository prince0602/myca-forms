package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.Membership;
import com.mynx.myca500.patient_forms.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);

    List<Membership> findByWorkspaceIdAndStatusOrderByJoinedAt(UUID workspaceId, MembershipStatus status);

    @Query("""
           select m from Membership m
            where m.userId = :userId
              and m.status = :status
              and (m.accessEndsAt is null or m.accessEndsAt > :now)
           """)
    List<Membership> findAccessibleByUserId(@Param("userId") UUID userId,
                                            @Param("status") MembershipStatus status,
                                            @Param("now") Instant now);

    @Modifying
    @Query("""
           update Membership m
              set m.status = :expired, m.endedAt = :now, m.updatedAt = :now
            where m.status = :active
              and m.accessEndsAt is not null
              and m.accessEndsAt <= :now
           """)
    int expireDueMemberships(@Param("active") MembershipStatus active,
                             @Param("expired") MembershipStatus expired,
                             @Param("now") Instant now);
}
