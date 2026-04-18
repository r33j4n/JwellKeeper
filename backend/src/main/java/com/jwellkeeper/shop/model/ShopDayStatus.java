package com.jwellkeeper.shop.model;

import com.jwellkeeper.common.model.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shop_day_status")
public class ShopDayStatus extends TenantScopedEntity {

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShopStatus status = ShopStatus.OPEN;

    @Column(name = "opened_by", columnDefinition = "char(36)")
    private UUID openedBy;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_by", columnDefinition = "char(36)")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
