package com.jwellkeeper.audit.model;

import com.jwellkeeper.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_audit_items")
public class StockAuditItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, columnDefinition = "char(36)")
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private StockAudit audit;

    @Column(name = "jewellery_id", nullable = false, columnDefinition = "char(36)")
    private UUID jewelleryId;

    @Column(name = "scanned", nullable = false)
    private boolean scanned;

    @Column(name = "scanned_at")
    private Instant scannedAt;

    @Column(name = "scanned_by", columnDefinition = "char(36)")
    private UUID scannedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", nullable = false, length = 40)
    private AuditItemResolution resolution = AuditItemResolution.PENDING;

    @Column(name = "resolution_changed_at")
    private Instant resolutionChangedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @PrePersist
    void prePersistAuditItem() {
        ensureId();
    }
}
