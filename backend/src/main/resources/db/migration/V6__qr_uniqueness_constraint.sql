-- Add unique constraint on QR payload token per tenant
-- This ensures that each jewellery item has a unique QR code within their tenant
ALTER TABLE jewellery
    ADD UNIQUE KEY uk_jewellery_tenant_qr_token (tenant_id, qr_payload_token(255));
