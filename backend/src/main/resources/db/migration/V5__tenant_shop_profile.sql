ALTER TABLE tenants
    ADD COLUMN shop_address VARCHAR(500) NULL AFTER shop_name,
    ADD COLUMN shop_contact_number VARCHAR(40) NULL AFTER shop_address,
    ADD COLUMN shop_email VARCHAR(180) NULL AFTER shop_contact_number,
    ADD COLUMN tax_number VARCHAR(80) NULL AFTER shop_email,
    ADD COLUMN receipt_footer_note VARCHAR(500) NULL AFTER tax_number,
    ADD COLUMN logo_storage_key VARCHAR(700) NULL AFTER receipt_footer_note,
    ADD COLUMN logo_mime_type VARCHAR(80) NULL AFTER logo_storage_key;
