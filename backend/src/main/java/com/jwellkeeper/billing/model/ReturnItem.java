package com.jwellkeeper.billing.model;

import com.jwellkeeper.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
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
@Table(name = "return_items")
public class ReturnItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, columnDefinition = "char(36)")
    private UUID tenantId;

    @Column(name = "return_id", nullable = false, columnDefinition = "char(36)")
    private UUID returnId;

    @Column(name = "bill_item_id", nullable = false, columnDefinition = "char(36)")
    private UUID billItemId;

    @Column(name = "jewellery_id", nullable = false, columnDefinition = "char(36)")
    private UUID jewelleryId;

    @Column(name = "refund_amount", precision = 19, scale = 4)
    private BigDecimal refundAmount;

    @Column(name = "restock", nullable = false)
    private boolean restock = true;

    @PrePersist
    void prePersistReturnItem() {
        ensureId();
    }
}
