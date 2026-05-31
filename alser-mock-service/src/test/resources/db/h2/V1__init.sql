CREATE TABLE device_offer (
    id             uuid PRIMARY KEY DEFAULT random_uuid(),
    sku            varchar(64) NOT NULL UNIQUE,
    name           varchar(255) NOT NULL,
    base_price     numeric(15,2) NOT NULL,
    discount_pct   numeric(5,2) NOT NULL,
    audience_tags  clob NOT NULL,
    valid_until    timestamptz NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE applied_discount (
    id           uuid PRIMARY KEY DEFAULT random_uuid(),
    user_id      varchar(64) NOT NULL,
    offer_id     uuid NOT NULL REFERENCES device_offer(id),
    final_price  numeric(15,2) NOT NULL,
    applied_at   timestamptz NOT NULL DEFAULT now()
);
