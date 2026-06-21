package com.jwellkeeper.jewellery.model;

import com.jwellkeeper.common.model.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment extends TenantScopedEntity {

    @Column(name = "jewellery_id", nullable = false, columnDefinition = "char(36)")
    private UUID jewelleryId;

    @Column(name = "before_type_id", columnDefinition = "char(36)")
    private UUID beforeTypeId;

    @Column(name = "after_type_id", columnDefinition = "char(36)")
    private UUID afterTypeId;

    @Column(name = "before_karat", length = 16)
    private String beforeKarat;

    @Column(name = "after_karat", length = 16)
    private String afterKarat;

    @Column(name = "before_weight", precision = 12, scale = 3)
    private BigDecimal beforeWeight;

    @Column(name = "before_design_name", length = 160)
    private String beforeDesignName;

    @Column(name = "after_design_name", length = 160)
    private String afterDesignName;

    @Column(name = "before_notes", length = 1000)
    private String beforeNotes;

    @Column(name = "after_notes", length = 1000)
    private String afterNotes;

    @Column(name = "after_weight", precision = 12, scale = 3)
    private BigDecimal afterWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", length = 30)
    private JewelleryStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status", length = 30)
    private JewelleryStatus afterStatus;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "created_by", nullable = false, columnDefinition = "char(36)")
    private UUID createdBy;
}
