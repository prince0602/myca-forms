package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.entity.AuditEvent;
import com.mynx.myca500.patient_forms.enums.AuditAction;
import com.mynx.myca500.patient_forms.model.ClientInfo;
import com.mynx.myca500.patient_forms.repository.AuditEventRepository;
import com.mynx.myca500.patient_forms.util.ClientInfoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditAction action, UUID actorUserId, UUID workspaceId, String target, Map<String, Object> details) {
        ClientInfo client = ClientInfoUtil.current();

        AuditEvent event = new AuditEvent();
        event.setOccurredAt(clock.instant());
        event.setAction(action);
        event.setActorUserId(actorUserId);
        event.setWorkspaceId(workspaceId);
        event.setTarget(target);
        event.setIp(client.ip());
        event.setUserAgent(client.userAgent());
        event.setDetails(details == null || details.isEmpty() ? null : details);
        auditEventRepository.save(event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditAction action, UUID actorUserId, UUID workspaceId, String target) {
        record(action, actorUserId, workspaceId, target, Map.of());
    }
}
