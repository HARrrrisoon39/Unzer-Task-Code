-- Add missing columns to cart_item (sku and name are mapped by the CartItem entity)
ALTER TABLE cart_item ADD COLUMN IF NOT EXISTS sku  VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE cart_item ADD COLUMN IF NOT EXISTS name VARCHAR(255) NOT NULL DEFAULT '';

-- Remove the outbox_event table — no corresponding Java code exists
DROP TABLE IF EXISTS outbox_event;

-- Store the Unzer charge ID on payment for use in refunds
ALTER TABLE payment ADD COLUMN IF NOT EXISTS unzer_charge_id VARCHAR(100);
