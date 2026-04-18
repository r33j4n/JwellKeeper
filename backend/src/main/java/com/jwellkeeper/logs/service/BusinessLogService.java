package com.jwellkeeper.logs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwellkeeper.logs.model.BusinessLog;
import com.jwellkeeper.logs.repository.BusinessLogRepository;
import com.jwellkeeper.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessLogService {

    private final BusinessLogRepository repository;
    private final ObjectMapper objectMapper;

    public void log(String action, String entityType, UUID entityId, String result, String message, Map<String, ?> metadata) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorUserId = TenantContext.current().map(user -> user.userId()).orElse(null);
        logForTenant(tenantId, actorUserId, action, entityType, entityId, result, message, metadata);
    }

    public void logForTenant(
            UUID tenantId,
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String result,
            String message,
            Map<String, ?> metadata
    ) {
        try {
            BusinessLog businessLog = new BusinessLog();
            businessLog.setTenantId(tenantId);
            businessLog.setActorUserId(actorUserId);
            businessLog.setAction(action);
            businessLog.setEntityType(entityType);
            businessLog.setEntityId(entityId);
            businessLog.setResult(result);
            businessLog.setMessage(message);
            businessLog.setMetadata(toJson(metadata));
            enrichRequest(businessLog);
            repository.save(businessLog);
        } catch (Exception ex) {
            log.warn("Failed to persist business log action={} entityType={} entityId={}", action, entityType, entityId, ex);
        }
    }

    private String toJson(Map<String, ?> metadata) throws JsonProcessingException {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(metadata);
    }

    private void enrichRequest(BusinessLog businessLog) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            businessLog.setIpAddress(request.getRemoteAddr());
            businessLog.setUserAgent(request.getHeader("User-Agent"));
        }
    }
}
