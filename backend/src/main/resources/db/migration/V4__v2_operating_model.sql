CREATE TABLE shop_day_status (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    business_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    opened_by CHAR(36) NULL,
    opened_at TIMESTAMP NULL,
    closed_by CHAR(36) NULL,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_day_status_tenant_date (tenant_id, business_date),
    KEY idx_shop_day_status_tenant_id (tenant_id),
    KEY idx_shop_day_status_business_date (business_date),
    KEY idx_shop_day_status_status (status),
    CONSTRAINT fk_shop_day_status_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_shop_day_status_opened_by FOREIGN KEY (opened_by) REFERENCES users (id),
    CONSTRAINT fk_shop_day_status_closed_by FOREIGN KEY (closed_by) REFERENCES users (id)
);

ALTER TABLE stock_audits
    ADD COLUMN stage VARCHAR(40) NOT NULL DEFAULT 'FINALIZED' AFTER status,
    ADD COLUMN started_by CHAR(36) NULL AFTER stage,
    ADD COLUMN started_at TIMESTAMP NULL AFTER started_by,
    ADD COLUMN force_closed BOOLEAN NOT NULL DEFAULT FALSE AFTER closed_at,
    ADD COLUMN force_closed_by CHAR(36) NULL AFTER force_closed,
    ADD COLUMN force_closed_at TIMESTAMP NULL AFTER force_closed_by,
    ADD COLUMN force_close_reason VARCHAR(1000) NULL AFTER force_closed_at,
    ADD COLUMN repeat_reason VARCHAR(1000) NULL AFTER force_close_reason,
    ADD COLUMN repeat_of_audit_id CHAR(36) NULL AFTER repeat_reason,
    ADD COLUMN expected_count BIGINT NOT NULL DEFAULT 0 AFTER repeat_of_audit_id,
    ADD COLUMN expected_total_weight DECIMAL(12, 3) NOT NULL DEFAULT 0 AFTER expected_count,
    ADD KEY idx_stock_audits_stage (stage),
    ADD KEY idx_stock_audits_started_by (started_by),
    ADD KEY idx_stock_audits_force_closed_by (force_closed_by),
    ADD CONSTRAINT fk_stock_audits_started_by FOREIGN KEY (started_by) REFERENCES users (id),
    ADD CONSTRAINT fk_stock_audits_force_closed_by FOREIGN KEY (force_closed_by) REFERENCES users (id),
    ADD CONSTRAINT fk_stock_audits_repeat_of FOREIGN KEY (repeat_of_audit_id) REFERENCES stock_audits (id);

UPDATE stock_audits
SET stage = CASE WHEN status = 'OPEN' THEN 'SCANNING' ELSE 'FINALIZED' END;

UPDATE stock_audits a
SET expected_count = (
        SELECT COUNT(*)
        FROM stock_audit_items i
        WHERE i.audit_id = a.id
    ),
    expected_total_weight = (
        SELECT COALESCE(SUM(j.weight), 0)
        FROM stock_audit_items i
        JOIN jewellery j ON j.id = i.jewellery_id
        WHERE i.audit_id = a.id
    );

ALTER TABLE stock_audit_items
    ADD COLUMN scanned_by CHAR(36) NULL AFTER scanned_at,
    ADD COLUMN resolution_changed_at TIMESTAMP NULL AFTER resolution,
    ADD KEY idx_stock_audit_items_resolution (resolution),
    ADD KEY idx_stock_audit_items_scanned_by (scanned_by),
    ADD CONSTRAINT fk_stock_audit_items_scanned_by FOREIGN KEY (scanned_by) REFERENCES users (id);

UPDATE stock_audit_items
SET resolution = CASE
    WHEN resolution = 'FOUND' THEN 'FOUND_IN_AUDIT'
    WHEN resolution = 'MANUALLY_CLOSED_MISSING' THEN 'MARKED_MISSING_ON_CLOSE'
    WHEN resolution = 'MISSING' THEN 'MARKED_MISSING_ON_CLOSE'
    ELSE 'PENDING'
END;

ALTER TABLE stock_audit_items
    MODIFY COLUMN resolution VARCHAR(40) NOT NULL DEFAULT 'PENDING';

CREATE TABLE stock_movements (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    jewellery_id CHAR(36) NOT NULL,
    movement_type VARCHAR(40) NOT NULL,
    source_type VARCHAR(80) NOT NULL,
    source_id CHAR(36) NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NULL,
    weight DECIMAL(12, 3) NULL,
    actor_user_id CHAR(36) NULL,
    reason VARCHAR(1000) NULL,
    metadata JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_stock_movements_tenant_id (tenant_id),
    KEY idx_stock_movements_jewellery_id (jewellery_id),
    KEY idx_stock_movements_type (movement_type),
    KEY idx_stock_movements_created_at (created_at),
    CONSTRAINT fk_stock_movements_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_stock_movements_jewellery FOREIGN KEY (jewellery_id) REFERENCES jewellery (id),
    CONSTRAINT fk_stock_movements_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

CREATE TABLE stock_adjustments (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    jewellery_id CHAR(36) NOT NULL,
    before_type_id CHAR(36) NULL,
    after_type_id CHAR(36) NULL,
    before_karat VARCHAR(16) NULL,
    after_karat VARCHAR(16) NULL,
    before_weight DECIMAL(12, 3) NULL,
    after_weight DECIMAL(12, 3) NULL,
    before_status VARCHAR(30) NULL,
    after_status VARCHAR(30) NULL,
    reason VARCHAR(1000) NOT NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_stock_adjustments_tenant_id (tenant_id),
    KEY idx_stock_adjustments_jewellery_id (jewellery_id),
    KEY idx_stock_adjustments_created_at (created_at),
    CONSTRAINT fk_stock_adjustments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_stock_adjustments_jewellery FOREIGN KEY (jewellery_id) REFERENCES jewellery (id),
    CONSTRAINT fk_stock_adjustments_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE jewellery_images (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    jewellery_id CHAR(36) NOT NULL,
    storage_key VARCHAR(700) NOT NULL,
    thumbnail_storage_key VARCHAR(700) NULL,
    capture_source VARCHAR(20) NOT NULL,
    mime_type VARCHAR(80) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    width INT NULL,
    height INT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by CHAR(36) NULL,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    KEY idx_jewellery_images_tenant_id (tenant_id),
    KEY idx_jewellery_images_jewellery_id (jewellery_id),
    KEY idx_jewellery_images_deleted_at (deleted_at),
    CONSTRAINT fk_jewellery_images_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_jewellery_images_jewellery FOREIGN KEY (jewellery_id) REFERENCES jewellery (id),
    CONSTRAINT fk_jewellery_images_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_jewellery_images_deleted_by FOREIGN KEY (deleted_by) REFERENCES users (id)
);

ALTER TABLE bills
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' AFTER bill_date,
    ADD KEY idx_bills_status (status);

CREATE TABLE bill_voids (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    bill_id CHAR(36) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bill_voids_bill_id (bill_id),
    KEY idx_bill_voids_tenant_id (tenant_id),
    CONSTRAINT fk_bill_voids_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_bill_voids_bill FOREIGN KEY (bill_id) REFERENCES bills (id),
    CONSTRAINT fk_bill_voids_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE bill_returns (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    bill_id CHAR(36) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    refund_amount DECIMAL(19, 4) NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_bill_returns_tenant_id (tenant_id),
    KEY idx_bill_returns_bill_id (bill_id),
    CONSTRAINT fk_bill_returns_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_bill_returns_bill FOREIGN KEY (bill_id) REFERENCES bills (id),
    CONSTRAINT fk_bill_returns_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE return_items (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    return_id CHAR(36) NOT NULL,
    bill_item_id CHAR(36) NOT NULL,
    jewellery_id CHAR(36) NOT NULL,
    refund_amount DECIMAL(19, 4) NULL,
    restock BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_return_items_tenant_id (tenant_id),
    KEY idx_return_items_return_id (return_id),
    KEY idx_return_items_jewellery_id (jewellery_id),
    CONSTRAINT fk_return_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_return_items_return FOREIGN KEY (return_id) REFERENCES bill_returns (id),
    CONSTRAINT fk_return_items_bill_item FOREIGN KEY (bill_item_id) REFERENCES bill_items (id),
    CONSTRAINT fk_return_items_jewellery FOREIGN KEY (jewellery_id) REFERENCES jewellery (id)
);

CREATE TABLE exchanges (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    bill_id CHAR(36) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSON NULL,
    PRIMARY KEY (id),
    KEY idx_exchanges_tenant_id (tenant_id),
    KEY idx_exchanges_bill_id (bill_id),
    CONSTRAINT fk_exchanges_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_exchanges_bill FOREIGN KEY (bill_id) REFERENCES bills (id),
    CONSTRAINT fk_exchanges_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);
