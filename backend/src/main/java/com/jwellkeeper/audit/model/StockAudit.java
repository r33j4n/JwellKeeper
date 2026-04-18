package com.jwellkeeper.audit.model;

import com.jwellkeeper.common.model.TenantScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_audits")
public class StockAudit extends TenantScopedEntity {

    @Column(name = "audit_date", nullable = false)
    private LocalDate auditDate;

    @Column(name = "run_number", nullable = false)
    private int runNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockAuditStatus status = StockAuditStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 40)
    private StockAuditStage stage = StockAuditStage.SCANNING;

    @Column(name = "started_by", columnDefinition = "char(36)")
    private UUID startedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "manually_closed", nullable = false)
    private boolean manuallyClosed;

    @Column(name = "closed_by", columnDefinition = "char(36)")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "force_closed", nullable = false)
    private boolean forceClosed;

    @Column(name = "force_closed_by", columnDefinition = "char(36)")
    private UUID forceClosedBy;

    @Column(name = "force_closed_at")
    private Instant forceClosedAt;

    @Column(name = "force_close_reason", length = 1000)
    private String forceCloseReason;

    @Column(name = "repeat_reason", length = 1000)
    private String repeatReason;

    @Column(name = "repeat_of_audit_id", columnDefinition = "char(36)")
    private UUID repeatOfAuditId;

    @Column(name = "expected_count", nullable = false)
    private long expectedCount;

    @Column(name = "expected_total_weight", nullable = false, precision = 12, scale = 3)
    private BigDecimal expectedTotalWeight = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockAuditItem> items = new ArrayList<>();

    public void addItem(StockAuditItem item) {
        item.setAudit(this);
        items.add(item);
    }
}
