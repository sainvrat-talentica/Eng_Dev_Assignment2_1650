-- Assignment 1 operational schema (public) with Assignment 2 shared fields baked in.
-- Field names align with third-assignment-sample-data-set CSV columns where applicable.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- B2B client (maps analytics.client / clients.csv)
CREATE TABLE business_client (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     BIGINT UNIQUE,              -- optional link to imported sample client_id
    client_name     VARCHAR(255) NOT NULL,
    gst_number      VARCHAR(50),
    contact_person  VARCHAR(255),
    contact_phone   VARCHAR(30),
    contact_email   VARCHAR(255),
    address_line1   TEXT,
    address_line2   TEXT,
    city            VARCHAR(100),
    state           VARCHAR(100),
    pincode         VARCHAR(20),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cuisine (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Restaurant = kitchen/warehouse in Assignment 2
CREATE TABLE restaurant (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id         BIGINT UNIQUE,          -- optional link to analytics.warehouse.warehouse_id
    name                VARCHAR(255) NOT NULL,
    address_line1       TEXT NOT NULL,
    address_line2       TEXT,
    city                VARCHAR(100) NOT NULL,
    state               VARCHAR(100) NOT NULL DEFAULT 'Maharashtra',
    pincode             VARCHAR(20),
    rating              DECIMAL(2, 1) DEFAULT 0.0,
    is_open             BOOLEAN NOT NULL DEFAULT false,
    estimated_wait_mins INTEGER DEFAULT 30,
    capacity            INTEGER,                -- A2 warehouse capacity
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    contact_email       VARCHAR(255),
    opening_time        TIME,
    closing_time        TIME,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_restaurant_city ON restaurant(city);
CREATE INDEX idx_restaurant_is_open ON restaurant(is_open);
CREATE INDEX idx_restaurant_name_trgm ON restaurant USING gin(name gin_trgm_ops);

CREATE TABLE restaurant_cuisine (
    restaurant_id UUID NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    cuisine_id    UUID NOT NULL REFERENCES cuisine(id) ON DELETE CASCADE,
    PRIMARY KEY (restaurant_id, cuisine_id)
);

CREATE TABLE menu_item (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    category      VARCHAR(100) NOT NULL,
    price         DECIMAL(10, 2) NOT NULL,
    available     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_menu_item_restaurant ON menu_item(restaurant_id);
CREATE INDEX idx_menu_item_available ON menu_item(restaurant_id, available);

CREATE TABLE customer (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255),
    phone      VARCHAR(30),
    email      VARCHAR(255),
    client_id  UUID REFERENCES business_client(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE driver (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id      BIGINT UNIQUE,
    name             VARCHAR(255) NOT NULL,
    phone            VARCHAR(30),
    license_number   VARCHAR(20),
    partner_company  VARCHAR(50),
    city             VARCHAR(100),
    state            VARCHAR(100),
    status           VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    current_order_id UUID,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE "order" (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key         VARCHAR(128) UNIQUE,
    customer_id             UUID NOT NULL REFERENCES customer(id),
    client_id               UUID REFERENCES business_client(id),
    restaurant_id           UUID NOT NULL REFERENCES restaurant(id),
    driver_id               UUID REFERENCES driver(id),
    customer_name           VARCHAR(255),
    customer_phone          VARCHAR(30),
    delivery_address_line1  TEXT NOT NULL,
    delivery_address_line2  TEXT,
    city                    VARCHAR(100) NOT NULL,
    state                   VARCHAR(100),
    pincode                 VARCHAR(20),
    status                  VARCHAR(30) NOT NULL,
    payment_status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_mode            VARCHAR(20),
    total_amount            DECIMAL(12, 2) NOT NULL,
    failure_reason          VARCHAR(50),
    delay_reason            VARCHAR(50),
    order_date              TIMESTAMPTZ NOT NULL DEFAULT now(),
    promised_delivery_at    TIMESTAMPTZ,
    actual_delivery_at      TIMESTAMPTZ,
    prep_started_at         TIMESTAMPTZ,
    prep_completed_at       TIMESTAMPTZ,
    out_for_delivery_at     TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    is_delayed              BOOLEAN NOT NULL DEFAULT false,
    is_failed               BOOLEAN NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_order_customer ON "order"(customer_id);
CREATE INDEX idx_order_client ON "order"(client_id);
CREATE INDEX idx_order_restaurant ON "order"(restaurant_id);
CREATE INDEX idx_order_status ON "order"(status);
CREATE INDEX idx_order_city_created ON "order"(city, created_at);
CREATE INDEX idx_order_idempotency ON "order"(idempotency_key);

CREATE TABLE order_item (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_item(id),
    name         VARCHAR(255) NOT NULL,
    quantity     INTEGER NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    line_total   DECIMAL(10, 2) NOT NULL
);

CREATE TABLE order_state_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    changed_by  VARCHAR(100),
    reason      VARCHAR(50),
    notes       TEXT,
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_history_order ON order_state_history(order_id, changed_at);

-- Kitchen prep log (maps analytics.warehouse_log / warehouse_logs.csv)
CREATE TABLE kitchen_prep_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    restaurant_id  UUID NOT NULL REFERENCES restaurant(id),
    picking_start  TIMESTAMPTZ,
    picking_end    TIMESTAMPTZ,
    dispatch_time  TIMESTAMPTZ,
    notes          VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_kitchen_prep_order ON kitchen_prep_log(order_id);
CREATE INDEX idx_kitchen_prep_restaurant ON kitchen_prep_log(restaurant_id);

-- Fleet delivery log (maps analytics.fleet_log / fleet_logs.csv)
CREATE TABLE fleet_delivery_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    driver_id       UUID REFERENCES driver(id),
    vehicle_number  VARCHAR(20),
    route_code      VARCHAR(20),
    gps_delay_notes VARCHAR(100),
    departure_time  TIMESTAMPTZ,
    arrival_time    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fleet_delivery_order ON fleet_delivery_log(order_id);

-- Customer feedback (maps analytics.feedback / feedback.csv)
CREATE TABLE customer_feedback (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    customer_id   UUID REFERENCES customer(id),
    customer_name VARCHAR(255),
    feedback_text TEXT NOT NULL,
    sentiment     VARCHAR(20),
    rating        INTEGER,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_customer_feedback_order ON customer_feedback(order_id);

-- Per-order external context (maps analytics.external_factor / external_factors.csv)
CREATE TABLE order_external_factor (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id           UUID NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    traffic_condition  VARCHAR(20),
    weather_condition  VARCHAR(20),
    event_type         VARCHAR(20),
    recorded_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_external_factor_order ON order_external_factor(order_id);

-- GPS cold-path archive (live platform; sampled from Kafka)
CREATE TABLE driver_location_archive (
    id          BIGSERIAL PRIMARY KEY,
    driver_id   UUID NOT NULL,
    order_id    UUID REFERENCES "order"(id),
    latitude    DECIMAL(10, 7) NOT NULL,
    longitude   DECIMAL(10, 7) NOT NULL,
    heading     DECIMAL(5, 2),
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_gps_archive_driver_time ON driver_location_archive(driver_id, recorded_at);
CREATE INDEX idx_gps_archive_order ON driver_location_archive(order_id);

CREATE TABLE outbox_event (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_event(published_at) WHERE published_at IS NULL;
