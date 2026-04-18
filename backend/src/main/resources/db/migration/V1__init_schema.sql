CREATE TABLE tenants (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    shop_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenants_tenant_id (tenant_id)
);

CREATE TABLE tenant_settings (
    tenant_id CHAR(36) NOT NULL,
    default_currency_code CHAR(3) NOT NULL,
    bill_prefix VARCHAR(20) NOT NULL,
    bill_number_format VARCHAR(80) NOT NULL,
    next_bill_sequence BIGINT NOT NULL,
    sequence_reset_policy VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (tenant_id),
    CONSTRAINT fk_tenant_settings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE users (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    name VARCHAR(140) NOT NULL,
    email VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_tenant_id (tenant_id),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE jewellery_types (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    is_custom BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jewellery_types_tenant_name (tenant_id, name),
    KEY idx_jewellery_types_tenant_id (tenant_id),
    CONSTRAINT fk_jewellery_types_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE jewellery (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    type_id CHAR(36) NOT NULL,
    karat VARCHAR(16) NOT NULL,
    weight DECIMAL(12, 3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    qr_payload_token VARCHAR(1200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sold_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_jewellery_tenant_id (tenant_id),
    KEY idx_jewellery_created_at (created_at),
    KEY idx_jewellery_status (status),
    KEY idx_jewellery_type_id (type_id),
    CONSTRAINT fk_jewellery_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_jewellery_type FOREIGN KEY (type_id) REFERENCES jewellery_types (id)
);

CREATE TABLE bills (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    bill_no VARCHAR(80) NOT NULL,
    bill_date DATE NOT NULL,
    currency_code CHAR(3) NOT NULL,
    total_amount DECIMAL(19, 4) NOT NULL,
    customer_name VARCHAR(160) NULL,
    customer_phone VARCHAR(40) NULL,
    customer_address VARCHAR(500) NULL,
    payment_method VARCHAR(80) NULL,
    notes VARCHAR(1000) NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bills_tenant_bill_no (tenant_id, bill_no),
    KEY idx_bills_tenant_id (tenant_id),
    KEY idx_bills_created_at (created_at),
    KEY idx_bills_bill_date (bill_date),
    CONSTRAINT fk_bills_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_bills_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE bill_items (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    bill_id CHAR(36) NOT NULL,
    jewellery_id CHAR(36) NOT NULL,
    type_name_snapshot VARCHAR(120) NOT NULL,
    karat_snapshot VARCHAR(16) NOT NULL,
    weight DECIMAL(12, 3) NOT NULL,
    final_price DECIMAL(19, 4) NOT NULL,
    rate_per_gram DECIMAL(19, 4) NULL,
    making_charge DECIMAL(19, 4) NULL,
    discount_amount DECIMAL(19, 4) NULL,
    tax_amount DECIMAL(19, 4) NULL,
    notes VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    KEY idx_bill_items_tenant_id (tenant_id),
    KEY idx_bill_items_bill_id (bill_id),
    KEY idx_bill_items_jewellery_id (jewellery_id),
    CONSTRAINT fk_bill_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_bill_items_bill FOREIGN KEY (bill_id) REFERENCES bills (id),
    CONSTRAINT fk_bill_items_jewellery FOREIGN KEY (jewellery_id) REFERENCES jewellery (id)
);

CREATE TABLE stock_audits (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    audit_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    manually_closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_by CHAR(36) NULL,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_audits_tenant_date (tenant_id, audit_date),
    KEY idx_stock_audits_tenant_id (tenant_id),
    KEY idx_stock_audits_audit_date (audit_date),
    KEY idx_stock_audits_status (status),
    CONSTRAINT fk_stock_audits_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_stock_audits_closed_by FOREIGN KEY (closed_by) REFERENCES users (id)
);

CREATE TABLE stock_audit_items (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    audit_id CHAR(36) NOT NULL,
    jewellery_id CHAR(36) NOT NULL,
    scanned BOOLEAN NOT NULL DEFAULT FALSE,
    scanned_at TIMESTAMP NULL,
    resolution VARCHAR(40) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_audit_items_audit_jewellery (audit_id, jewellery_id),
    KEY idx_stock_audit_items_tenant_id (tenant_id),
    KEY idx_stock_audit_items_audit_id (audit_id),
    KEY idx_stock_audit_items_jewellery_id (jewellery_id),
    CONSTRAINT fk_stock_audit_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_stock_audit_items_audit FOREIGN KEY (audit_id) REFERENCES stock_audits (id),
    CONSTRAINT fk_stock_audit_items_jewellery FOREIGN KEY (jewellery_id) REFERENCES jewellery (id)
);

CREATE TABLE business_logs (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    actor_user_id CHAR(36) NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NULL,
    entity_id CHAR(36) NULL,
    result VARCHAR(30) NOT NULL,
    message VARCHAR(1000) NULL,
    metadata JSON NULL,
    ip_address VARCHAR(80) NULL,
    user_agent VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_business_logs_tenant_id (tenant_id),
    KEY idx_business_logs_created_at (created_at),
    KEY idx_business_logs_action (action),
    KEY idx_business_logs_actor_user_id (actor_user_id),
    CONSTRAINT fk_business_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_business_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
);
