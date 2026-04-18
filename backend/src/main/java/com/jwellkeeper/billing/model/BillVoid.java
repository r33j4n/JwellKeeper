package com.jwellkeeper.billing.model;

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
@Table(name = "bill_voids")
public class BillVoid extends TenantScopedEntity {

    @Column(name = "bill_id", nullable = false, columnDefinition = "char(36)")
    private UUID billId;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "created_by", nullable = false, columnDefinition = "char(36)")
    private UUID createdBy;
}
