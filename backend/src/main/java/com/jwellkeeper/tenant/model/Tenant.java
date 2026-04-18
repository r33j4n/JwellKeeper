package com.jwellkeeper.tenant.model;

import com.jwellkeeper.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private UUID tenantId;

    @Column(name = "shop_name", nullable = false, length = 160)
    private String shopName;

    @Column(name = "shop_address", length = 500)
    private String shopAddress;

    @Column(name = "shop_contact_number", length = 40)
    private String shopContactNumber;

    @Column(name = "shop_email", length = 180)
    private String shopEmail;

    @Column(name = "tax_number", length = 80)
    private String taxNumber;

    @Column(name = "receipt_footer_note", length = 500)
    private String receiptFooterNote;

    @Column(name = "logo_storage_key", length = 700)
    private String logoStorageKey;

    @Column(name = "logo_mime_type", length = 80)
    private String logoMimeType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void initializeTenant() {
        ensureId();
        if (tenantId == null) {
            tenantId = getId();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
