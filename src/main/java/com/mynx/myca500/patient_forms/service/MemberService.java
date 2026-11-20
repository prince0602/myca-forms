package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.response.MemberResponse;
import com.mynx.myca500.patient_forms.entity.Membership;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.enums.AuditAction;
import com.mynx.myca500.patient_forms.enums.MembershipRole;
import com.mynx.myca500.patient_forms.enums.MembershipStatus;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.mapper.MemberMapper;
import com.mynx.myca500.patient_forms.model.WorkspaceAccess;
import com.mynx.myca500.patient_forms.repository.MembershipRepository;
import com.mynx.myca500.patient_forms.repository.UserRepository;
import com.mynx.myca500.patient_forms.util.PublicIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(User caller, String publicWorkspaceId) {
        WorkspaceAccess access = workspaceAccessService.requireMember(caller.getId(), publicWorkspaceId);
        List<Membership> memberships = membershipRepository.findByWorkspaceIdAndStatusOrderByJoinedAt(
                access.workspace().getId(), MembershipStatus.ACTIVE);

        Map<UUID, User> usersById = userRepository
                .findAllById(memberships.stream().map(Membership::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return memberships.stream()
                .map(m -> MemberMapper.toResponse(m, usersById.get(m.getUserId())))
                .toList();
    }

    @Transactional
    public void removeMember(User caller, String publicWorkspaceId, String memberAccountId) {
        WorkspaceAccess access = workspaceAccessService.requireOwner(caller.getId(), publicWorkspaceId);

        User target = userRepository.findByAccountId(PublicIdUtil.normalizeAccountId(memberAccountId))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Membership membership = membershipRepository.findByUserIdAndWorkspaceId(target.getId(), access.workspace().getId())
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (membership.getRole() == MembershipRole.OWNER) {
            throw new BusinessException(ErrorCode.CANNOT_REMOVE_OWNER);
        }

        membership.setStatus(MembershipStatus.REMOVED);
        membership.setEndedAt(clock.instant());

        auditService.record(AuditAction.MEMBER_REMOVED, caller.getId(), access.workspace().getId(), target.getAccountId());
    }
}
