package com.jwellkeeper.billing.model;

import com.jwellkeeper.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "bill_items")
public class BillItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, columnDefinition = "char(36)")
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "jewellery_id", nullable = false, columnDefinition = "char(36)")
    private UUID jewelleryId;

    @Column(name = "type_name_snapshot", nullable = false, length = 120)
    private String typeNameSnapshot;

    @Column(name = "design_name_snapshot", length = 160)
    private String designNameSnapshot;

    @Column(name = "karat_snapshot", nullable = false, length = 16)
    private String karatSnapshot;

    @Column(name = "weight", nullable = false, precision = 12, scale = 3)
    private BigDecimal weight;

    @Column(name = "final_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal finalPrice;

    @Column(name = "rate_per_gram", precision = 19, scale = 4)
    private BigDecimal ratePerGram;

    @Column(name = "making_charge", precision = 19, scale = 4)
    private BigDecimal makingCharge;

    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "notes", length = 1000)
    private String notes;

    @PrePersist
    void prePersistBillItem() {
        ensureId();
    }
}
