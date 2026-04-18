package com.jwellkeeper.stock.model;

import com.jwellkeeper.common.model.TenantScopedEntity;
import com.jwellkeeper.jewellery.model.JewelleryStatus;
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
@Table(name = "stock_movements")
public class StockMovement extends TenantScopedEntity {

    @Column(name = "jewellery_id", nullable = false, columnDefinition = "char(36)")
    private UUID jewelleryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private StockMovementType movementType;

    @Column(name = "source_type", nullable = false, length = 80)
    private String sourceType;

    @Column(name = "source_id", columnDefinition = "char(36)")
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private JewelleryStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private JewelleryStatus toStatus;

    @Column(name = "weight", precision = 12, scale = 3)
    private BigDecimal weight;

    @Column(name = "actor_user_id", columnDefinition = "char(36)")
    private UUID actorUserId;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;
}
