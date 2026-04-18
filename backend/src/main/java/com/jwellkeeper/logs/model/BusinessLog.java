package com.jwellkeeper.logs.model;

import com.jwellkeeper.common.model.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "business_logs")
public class BusinessLog extends TenantScopedEntity {

    @Column(name = "actor_user_id", columnDefinition = "char(36)")
    private UUID actorUserId;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", length = 80)
    private String entityType;

    @Column(name = "entity_id", columnDefinition = "char(36)")
    private UUID entityId;

    @Column(name = "result", nullable = false, length = 30)
    private String result;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;
}
