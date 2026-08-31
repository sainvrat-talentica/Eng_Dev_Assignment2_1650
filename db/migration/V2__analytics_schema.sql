-- Assignment 2 analytics schema — mirrors third-assignment-sample-data-set CSV files

CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE analytics.client (
    client_id       BIGINT PRIMARY KEY,
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
    created_at      TIMESTAMPTZ
);

CREATE TABLE analytics.warehouse (
    warehouse_id    BIGINT PRIMARY KEY,
    warehouse_name  VARCHAR(100) NOT NULL,
    state           VARCHAR(100),
    city            VARCHAR(100),
    pincode         VARCHAR(20),
    capacity        INTEGER,
    manager_name    VARCHAR(255),
    contact_phone   VARCHAR(30),
    created_at      TIMESTAMPTZ
);

CREATE TABLE analytics.driver (
    driver_id       BIGINT PRIMARY KEY,
    driver_name     VARCHAR(255),
    phone           VARCHAR(30),
    license_number  VARCHAR(20),
    partner_company VARCHAR(50),
    city            VARCHAR(100),
    state           VARCHAR(100),
    status          VARCHAR(20),
    created_at      TIMESTAMPTZ
);

CREATE TABLE analytics."order" (
    order_id                BIGINT PRIMARY KEY,
    client_id               BIGINT REFERENCES analytics.client(client_id),
    customer_name           VARCHAR(255),
    customer_phone          VARCHAR(30),
    delivery_address_line1  TEXT,
    delivery_address_line2  TEXT,
    city                    VARCHAR(100) NOT NULL,
    state                   VARCHAR(100),
    pincode                 VARCHAR(20),
    order_date              TIMESTAMPTZ,
    promised_delivery_date  TIMESTAMPTZ,
    actual_delivery_date    TIMESTAMPTZ,
    status                  VARCHAR(20) NOT NULL,
    payment_mode            VARCHAR(20),
    amount                  DECIMAL(12, 2),
    failure_reason          VARCHAR(50),
    created_at              TIMESTAMPTZ,
    is_delayed              BOOLEAN NOT NULL DEFAULT FALSE,
    is_failed               BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_sample_order_city ON analytics."order"(city);
CREATE INDEX idx_sample_order_client ON analytics."order"(client_id);
CREATE INDEX idx_sample_order_status ON analytics."order"(status);
CREATE INDEX idx_sample_order_dates ON analytics."order"(order_date);
CREATE INDEX idx_sample_order_failure ON analytics."order"(failure_reason) WHERE failure_reason IS NOT NULL;

CREATE TABLE analytics.warehouse_log (
    log_id          BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES analytics."order"(order_id),
    warehouse_id    BIGINT NOT NULL REFERENCES analytics.warehouse(warehouse_id),
    picking_start   TIMESTAMPTZ,
    picking_end     TIMESTAMPTZ,
    dispatch_time   TIMESTAMPTZ,
    notes           VARCHAR(255)
);

CREATE INDEX idx_wh_log_order ON analytics.warehouse_log(order_id);
CREATE INDEX idx_wh_log_warehouse ON analytics.warehouse_log(warehouse_id);

CREATE TABLE analytics.fleet_log (
    fleet_log_id    BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES analytics."order"(order_id),
    driver_id       BIGINT REFERENCES analytics.driver(driver_id),
    vehicle_number  VARCHAR(20),
    route_code      VARCHAR(20),
    gps_delay_notes VARCHAR(100),
    departure_time  TIMESTAMPTZ,
    arrival_time    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ
);

CREATE INDEX idx_fleet_log_order ON analytics.fleet_log(order_id);
CREATE INDEX idx_fleet_log_driver ON analytics.fleet_log(driver_id);

CREATE TABLE analytics.feedback (
    feedback_id     BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES analytics."order"(order_id),
    customer_name   VARCHAR(255),
    feedback_text   TEXT,
    sentiment       VARCHAR(20),
    rating          INTEGER,
    created_at      TIMESTAMPTZ
);

CREATE INDEX idx_feedback_order ON analytics.feedback(order_id);

CREATE TABLE analytics.external_factor (
    factor_id           BIGINT PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES analytics."order"(order_id),
    traffic_condition   VARCHAR(20),
    weather_condition   VARCHAR(20),
    event_type          VARCHAR(20),
    recorded_at         TIMESTAMPTZ
);

CREATE INDEX idx_ext_factor_order ON analytics.external_factor(order_id);
CREATE INDEX idx_ext_factor_event ON analytics.external_factor(event_type);

CREATE TABLE analytics.delivery_insight (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query_type      VARCHAR(50) NOT NULL,
    parameters      JSONB NOT NULL,
    narrative       TEXT NOT NULL,
    recommendations JSONB,
    evidence        JSONB,
    generated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
