ALTER TABLE stock_audits
    DROP INDEX uk_stock_audits_tenant_date;

ALTER TABLE stock_audits
    ADD COLUMN run_number INT NOT NULL DEFAULT 1 AFTER audit_date;

ALTER TABLE stock_audits
    ADD UNIQUE KEY uk_stock_audits_tenant_date_run (tenant_id, audit_date, run_number),
    ADD KEY idx_stock_audits_tenant_date_run (tenant_id, audit_date, run_number);
