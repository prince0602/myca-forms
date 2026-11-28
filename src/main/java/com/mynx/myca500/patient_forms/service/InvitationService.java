package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.EmailDto;
import com.mynx.myca500.patient_forms.dto.response.AcceptInvitationResponse;
import com.mynx.myca500.patient_forms.dto.response.InvitationPreviewResponse;
import com.mynx.myca500.patient_forms.dto.response.InvitationPreviewResponse.InviterRef;
import com.mynx.myca500.patient_forms.dto.response.InvitationPreviewResponse.WorkspaceRef;
import com.mynx.myca500.patient_forms.dto.response.InvitationResponse;
import com.mynx.myca500.patient_forms.entity.Invitation;
import com.mynx.myca500.patient_forms.entity.Membership;
import com.mynx.myca500.patient_forms.entity.User;
import com.mynx.myca500.patient_forms.entity.Workspace;
import com.mynx.myca500.patient_forms.enums.AuditAction;
import com.mynx.myca500.patient_forms.enums.InvitationStatus;
import com.mynx.myca500.patient_forms.enums.MembershipRole;
import com.mynx.myca500.patient_forms.enums.MembershipStatus;
import com.mynx.myca500.patient_forms.enums.OnboardingStatus;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.exception.ResourceCreationException;
import com.mynx.myca500.patient_forms.model.WorkspaceAccess;
import com.mynx.myca500.patient_forms.repository.InvitationRepository;
import com.mynx.myca500.patient_forms.repository.MembershipRepository;
import com.mynx.myca500.patient_forms.repository.UserRepository;
import com.mynx.myca500.patient_forms.repository.WorkspaceRepository;
import com.mynx.myca500.patient_forms.util.EmailUtil;
import com.mynx.myca500.patient_forms.util.NameUtil;
import com.mynx.myca500.patient_forms.util.PublicIdUtil;
import com.mynx.myca500.patient_forms.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final String EMAIL_TEMPLATE = "workspace-invite";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final int MAX_TOKEN_LENGTH = 200;
    private static final String FALLBACK_INVITER_NAME = "The workspace owner";
    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneOffset.UTC);

    private final InvitationRepository invitationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final Clock clock;

    @Value("${app.environment-url}")
    private String environmentUrl;

    @Value("${invitations.expiration-days:7}")
    private int expirationDays;


    @Value("${invitations.resend-limit-per-hour:3}")
    private int resendLimitPerHour;


    @Transactional
    public InvitationResponse create(User caller, String publicWorkspaceId, String rawEmail) {
        WorkspaceAccess access = workspaceAccessService.requireOwner(caller.getId(), publicWorkspaceId);
        Workspace workspace = access.workspace();
        if (!workspace.getPlan().allowsInvites()) {
            throw new BusinessException(ErrorCode.PLAN_INVITES_NOT_ALLOWED);
        }

        String email = EmailUtil.normalize(rawEmail);
        if (email.equals(caller.getEmail())) {
            throw new BusinessException(ErrorCode.CANNOT_INVITE_SELF);
        }
        if (isActiveMember(email, workspace.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_MEMBER);
        }

        Instant now = clock.instant();
        long recent = invitationRepository.countByWorkspaceIdAndEmailAndCreatedAtAfter(
                workspace.getId(), email, now.minus(Duration.ofHours(1)));
        if (recent >= resendLimitPerHour) {
            throw new BusinessException(ErrorCode.INVITATION_RESEND_LIMIT);
        }

        invitationRepository.findByWorkspaceIdAndEmailAndStatus(workspace.getId(), email, InvitationStatus.PENDING)
                .ifPresent(old -> {
                    old.setStatus(InvitationStatus.REVOKED);
                    invitationRepository.saveAndFlush(old);
                });
        requireFreeSeat(workspace, now, true);

        String token = TokenUtil.generate();
        Invitation invitation = new Invitation();
        invitation.setWorkspaceId(workspace.getId());
        invitation.setEmail(email);
        invitation.setTokenHash(TokenUtil.sha256Hex(token));
        invitation.setRole(MembershipRole.MEMBER);
        invitation.setInvitedBy(caller.getId());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(now.plus(Duration.ofDays(expirationDays)));
        try {
            invitationRepository.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_PENDING);
        }

        sendEmail(invitation, token, workspace, caller);
        auditService.record(AuditAction.INVITATION_SENT, caller.getId(), workspace.getId(), EmailUtil.mask(email),
                Map.of("invitationId", invitation.getId().toString()));
        log.info("Invitation {} sent for workspace {}", invitation.getId(), workspace.getWorkspaceId());

        return toResponse(invitation, caller, now);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listPending(User caller, String publicWorkspaceId) {
        WorkspaceAccess access = workspaceAccessService.requireOwner(caller.getId(), publicWorkspaceId);
        List<Invitation> pending = invitationRepository.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(
                access.workspace().getId(), InvitationStatus.PENDING);
        if (pending.isEmpty()) return List.of();

        Map<UUID, User> inviters = userRepository
                .findAllById(pending.stream().map(Invitation::getInvitedBy).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Instant now = clock.instant();
        return pending.stream().map(i -> toResponse(i, inviters.get(i.getInvitedBy()), now)).toList();
    }

    @Transactional
    public void revoke(User caller, String publicWorkspaceId, String invitationId) {
        WorkspaceAccess access = workspaceAccessService.requireOwner(caller.getId(), publicWorkspaceId);
        Invitation invitation = parseUuid(invitationId)
                .flatMap(id -> invitationRepository.findByIdAndWorkspaceId(id, access.workspace().getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));

        switch (invitation.getStatus()) {
            case REVOKED -> { /* idempotent */ }
            case ACCEPTED -> throw new BusinessException(ErrorCode.INVITATION_ALREADY_ACCEPTED);
            case PENDING -> {
                invitation.setStatus(InvitationStatus.REVOKED);
                auditService.record(AuditAction.INVITATION_REVOKED, caller.getId(), access.workspace().getId(),
                        EmailUtil.mask(invitation.getEmail()), Map.of("invitationId", invitation.getId().toString()));
            }
        }
    }


    @Transactional(readOnly = true)
    public InvitationPreviewResponse preview(String token) {
        Invitation invitation = findByToken(token);
        Workspace workspace = workspaceRepository.findById(invitation.getWorkspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
        String inviterName = userRepository.findById(invitation.getInvitedBy())
                .map(InvitationService::displayName)
                .orElse(FALLBACK_INVITER_NAME);

        return new InvitationPreviewResponse(
                effectiveStatus(invitation, clock.instant()),
                new WorkspaceRef(workspace.getWorkspaceId(), workspace.getName()),
                new InviterRef(inviterName),
                EmailUtil.mask(invitation.getEmail()),
                userRepository.findByEmail(invitation.getEmail()).isPresent(),
                invitation.getExpiresAt());
    }

    @Transactional
    public AcceptInvitationResponse accept(User caller, String token) {
        Invitation invitation = findByToken(token);
        Instant now = clock.instant();

        if (invitation.getStatus() == InvitationStatus.REVOKED) {
            throw new BusinessException(ErrorCode.INVITATION_REVOKED);
        }
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            if (!caller.getId().equals(invitation.getAcceptedBy())) {
                throw new BusinessException(ErrorCode.INVITATION_ALREADY_ACCEPTED);
            }
            return alreadyAccepted(invitation);
        }
        if (!now.isBefore(invitation.getExpiresAt())) {
            throw new BusinessException(ErrorCode.INVITATION_EXPIRED);
        }
        if (!invitation.getEmail().equals(caller.getEmail())) {
            throw new BusinessException(ErrorCode.INVITATION_EMAIL_MISMATCH);
        }
        if (caller.getOnboardingStatus() != OnboardingStatus.COMPLETE) {
            throw new BusinessException(ErrorCode.ONBOARDING_REQUIRED);
        }

        Workspace workspace = workspaceRepository.lockById(invitation.getWorkspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
        if (!workspace.getPlan().allowsInvites()) {
            throw new BusinessException(ErrorCode.PLAN_INVITES_NOT_ALLOWED);
        }
        boolean alreadyHasAccess = membershipRepository.findByUserIdAndWorkspaceId(caller.getId(), workspace.getId())
                .filter(workspaceAccessService::hasAccess)
                .isPresent();
        if (!alreadyHasAccess) {
            requireFreeSeat(workspace, now, false);
        }

        Membership membership = upsertMembership(caller.getId(), invitation, now);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedBy(caller.getId());
        invitation.setAcceptedAt(now);

        User user = userRepository.findById(caller.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setDefaultWorkspaceId(workspace.getId());

        auditService.record(AuditAction.INVITATION_ACCEPTED, caller.getId(), workspace.getId(), caller.getAccountId(),
                Map.of("invitationId", invitation.getId().toString()));

        return new AcceptInvitationResponse(new WorkspaceRef(workspace.getWorkspaceId(), workspace.getName()),
                membership.getRole(), membership.getJoinedAt());
    }


    private void requireFreeSeat(Workspace workspace, Instant now, boolean countPendingInvites) {
        long taken = membershipRepository.countAccessibleByWorkspaceId(workspace.getId(), MembershipStatus.ACTIVE, now);
        if (countPendingInvites) {
            taken += invitationRepository.countByWorkspaceIdAndStatusAndExpiresAtAfter(
                    workspace.getId(), InvitationStatus.PENDING, now);
        }
        if (taken >= workspace.getPlan().maxUsers()) {
            throw new BusinessException(ErrorCode.PLAN_SEAT_LIMIT_REACHED);
        }
    }

    private Membership upsertMembership(UUID userId, Invitation invitation, Instant now) {
        Membership membership = membershipRepository.findByUserIdAndWorkspaceId(userId, invitation.getWorkspaceId())
                .orElse(null);
        if (membership != null && workspaceAccessService.hasAccess(membership)) {
            return membership;
        }
        if (membership == null) {
            membership = new Membership();
            membership.setUserId(userId);
            membership.setWorkspaceId(invitation.getWorkspaceId());
        }
        membership.setRole(invitation.getRole());
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setInvitedBy(invitation.getInvitedBy());
        membership.setJoinedAt(now);
        membership.setAccessEndsAt(null);
        membership.setEndedAt(null);
        return membershipRepository.save(membership);
    }

    private AcceptInvitationResponse alreadyAccepted(Invitation invitation) {
        Workspace workspace = workspaceRepository.findById(invitation.getWorkspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
        Instant joinedAt = membershipRepository
                .findByUserIdAndWorkspaceId(invitation.getAcceptedBy(), invitation.getWorkspaceId())
                .map(Membership::getJoinedAt)
                .orElse(invitation.getAcceptedAt());
        return new AcceptInvitationResponse(new WorkspaceRef(workspace.getWorkspaceId(), workspace.getName()),
                invitation.getRole(), joinedAt);
    }

    private boolean isActiveMember(String email, UUID workspaceId) {
        return userRepository.findByEmail(email)
                .flatMap(u -> membershipRepository.findByUserIdAndWorkspaceId(u.getId(), workspaceId))
                .filter(workspaceAccessService::hasAccess)
                .isPresent();
    }

    private Invitation findByToken(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_FOUND);
        }
        return invitationRepository.findByTokenHash(TokenUtil.sha256Hex(token.trim()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
    }

    private void sendEmail(Invitation invitation, String token, Workspace workspace, User inviter) {
        String link = environmentUrl + "/invite/" + token;
        try {
            emailService.send(EmailDto.fromTemplate(invitation.getEmail(), EMAIL_TEMPLATE, Map.of(
                    "link", link,
                    "workspaceName", workspace.getName(),
                    "inviterName", displayName(inviter),
                    "expiresOn", EXPIRY_FORMAT.format(invitation.getExpiresAt()))));
        } catch (ResourceCreationException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private static InvitationResponse toResponse(Invitation i, User inviter, Instant now) {
        InvitationResponse.Inviter by = inviter == null ? null
                : new InvitationResponse.Inviter(PublicIdUtil.formatAccountId(inviter.getAccountId()), displayName(inviter));
        return new InvitationResponse(i.getId().toString(), i.getEmail(), i.getRole(),
                effectiveStatus(i, now), by, i.getCreatedAt(), i.getExpiresAt());
    }

    private static String effectiveStatus(Invitation i, Instant now) {
        if (i.getStatus() == InvitationStatus.PENDING && !now.isBefore(i.getExpiresAt())) return STATUS_EXPIRED;
        return i.getStatus().name();
    }

    private static String displayName(User u) {
        String name = NameUtil.fullName(u.getFirstName(), u.getLastName());
        return name.isBlank() ? FALLBACK_INVITER_NAME : name;
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
