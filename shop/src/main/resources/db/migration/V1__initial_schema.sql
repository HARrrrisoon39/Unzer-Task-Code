-- V1: Initial schema for Unzer E-Commerce vertical slice
-- Covers: customer, inventory, cart, order, payment domains

CREATE TABLE IF NOT EXISTS customer (
    id            UUID         NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'CUSTOMER',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS address (
    id          UUID         NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    customer_id UUID,
    street      VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    country     VARCHAR(100) NOT NULL,
    zip         VARCHAR(20)  NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Product catalog (minimal, read-only in the vertical slice)
CREATE TABLE IF NOT EXISTS product (
    id          UUID         NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    sku         VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS product_variant (
    id         UUID            NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    product_id UUID            NOT NULL,
    sku        VARCHAR(100)    NOT NULL UNIQUE,
    name       VARCHAR(255)    NOT NULL,
    price      DECIMAL(19, 4)  NOT NULL,
    currency   VARCHAR(3)      NOT NULL DEFAULT 'EUR'
);

-- Inventory — one row per variant, optimistic lock via version
CREATE TABLE IF NOT EXISTS inventory (
    variant_id UUID    NOT NULL PRIMARY KEY,
    available  INTEGER NOT NULL DEFAULT 0,
    reserved   INTEGER NOT NULL DEFAULT 0,
    version    INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_available_non_negative CHECK (available >= 0),
    CONSTRAINT chk_reserved_non_negative  CHECK (reserved  >= 0)
);

CREATE TABLE IF NOT EXISTS reservation (
    id         UUID        NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    variant_id UUID        NOT NULL,
    order_id   UUID,
    quantity   INTEGER     NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    status     VARCHAR(30) NOT NULL DEFAULT 'RESERVED'
);

-- Cart — stored in DB for simplicity; in production this lives in Redis
CREATE TABLE IF NOT EXISTS cart (
    id            UUID      NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    customer_id   UUID,
    session_token VARCHAR(255),
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart_item (
    id          UUID           NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    cart_id     UUID           NOT NULL,
    variant_id  UUID           NOT NULL,
    quantity    INTEGER        NOT NULL,
    unit_price  DECIMAL(19, 4) NOT NULL,
    currency    VARCHAR(3)     NOT NULL DEFAULT 'EUR'
);

-- Order lifecycle
CREATE TABLE IF NOT EXISTS shop_order (
    id                  UUID           NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    customer_id         UUID,
    status              VARCHAR(50)    NOT NULL DEFAULT 'CREATED',
    total_amount        DECIMAL(19, 4) NOT NULL,
    currency            VARCHAR(3)     NOT NULL DEFAULT 'EUR',
    street              VARCHAR(255),
    city                VARCHAR(100),
    country             VARCHAR(100),
    zip                 VARCHAR(20),
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_line (
    id         UUID           NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    order_id   UUID           NOT NULL,
    variant_id UUID           NOT NULL,
    sku        VARCHAR(100)   NOT NULL,
    name       VARCHAR(255)   NOT NULL,
    quantity   INTEGER        NOT NULL,
    unit_price DECIMAL(19, 4) NOT NULL,
    currency   VARCHAR(3)     NOT NULL DEFAULT 'EUR'
);

CREATE TABLE IF NOT EXISTS order_status_history (
    id            UUID        NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    order_id      UUID        NOT NULL,
    from_status   VARCHAR(50),
    to_status     VARCHAR(50) NOT NULL,
    reason        TEXT,
    occurred_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payment — one record per order
CREATE TABLE IF NOT EXISTS payment (
    id                UUID           NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    order_id          UUID           NOT NULL UNIQUE,
    unzer_payment_id  VARCHAR(100),
    unzer_type_id     VARCHAR(100),
    method            VARCHAR(50)    NOT NULL,
    status            VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    amount            DECIMAL(19, 4) NOT NULL,
    currency          VARCHAR(3)     NOT NULL DEFAULT 'EUR',
    idempotency_key   VARCHAR(255)   NOT NULL UNIQUE,
    redirect_url      TEXT,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- All raw webhook events — for idempotency and audit
CREATE TABLE IF NOT EXISTS payment_event (
    id                  UUID        NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    unzer_payment_id    VARCHAR(100) NOT NULL,
    event_type          VARCHAR(100) NOT NULL,
    raw_payload         TEXT         NOT NULL,
    retrieve_url        TEXT,
    received_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed           BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (unzer_payment_id, event_type)
);

-- Outbox for reliable event publishing
CREATE TABLE IF NOT EXISTS outbox_event (
    id           UUID        NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed    BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Seed data for demo
INSERT INTO product (id, sku, name, description) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'WIDGET-001', 'Premium Widget', 'A high-quality widget for all your widget needs'),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'GADGET-001', 'Smart Gadget', 'The smartest gadget on the market');

INSERT INTO product_variant (id, product_id, sku, name, price, currency) VALUES
    ('bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'WIDGET-001-STD', 'Premium Widget Standard', 29.99, 'EUR'),
    ('bbbbbbbb-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'WIDGET-001-PRO', 'Premium Widget Pro', 49.99, 'EUR'),
    ('bbbbbbbb-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000002', 'GADGET-001-STD', 'Smart Gadget Standard', 99.99, 'EUR');

INSERT INTO inventory (variant_id, available, reserved, version) VALUES
    ('bbbbbbbb-0000-0000-0000-000000000001', 100, 0, 0),
    ('bbbbbbbb-0000-0000-0000-000000000002', 50, 0, 0),
    ('bbbbbbbb-0000-0000-0000-000000000003', 25, 0, 0);
