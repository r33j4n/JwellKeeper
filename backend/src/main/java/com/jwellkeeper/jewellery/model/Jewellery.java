package com.jwellkeeper.jewellery.model;

import com.jwellkeeper.common.model.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jewellery")
public class Jewellery extends TenantScopedEntity {

    @Column(name = "type_id", nullable = false, columnDefinition = "char(36)")
    private UUID typeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", insertable = false, updatable = false)
    private JewelleryType type;

    @Column(name = "karat", nullable = false, length = 16)
    private String karat;

    @Column(name = "design_name", length = 160)
    private String designName;

    @Column(name = "weight", nullable = false, precision = 12, scale = 3)
    private BigDecimal weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JewelleryStatus status = JewelleryStatus.AVAILABLE;

    @Column(name = "qr_payload_token", nullable = false, length = 1200)
    private String qrPayloadToken;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "sold_at")
    private Instant soldAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
