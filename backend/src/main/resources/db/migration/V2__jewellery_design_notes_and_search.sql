ALTER TABLE jewellery
    ADD COLUMN design_name VARCHAR(160) NULL AFTER karat,
    ADD COLUMN notes VARCHAR(1000) NULL AFTER qr_payload_token;

ALTER TABLE bill_items
    ADD COLUMN design_name_snapshot VARCHAR(160) NULL AFTER type_name_snapshot;

CREATE INDEX idx_jewellery_tenant_karat ON jewellery (tenant_id, karat);
CREATE INDEX idx_jewellery_tenant_design_name ON jewellery (tenant_id, design_name);
CREATE INDEX idx_bills_tenant_customer_name ON bills (tenant_id, customer_name);
