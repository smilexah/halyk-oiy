CREATE TABLE travel_offer (
    id             uuid DEFAULT random_uuid() NOT NULL,
    kind           varchar(16) NOT NULL,
    destination    varchar(128) NOT NULL,
    base_price     numeric(15,2) NOT NULL,
    discount_pct   numeric(5,2) NOT NULL,
    audience_tags  clob NOT NULL,
    valid_until    timestamp NOT NULL,
    created_at     timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);

CREATE TABLE booking (
    id           uuid DEFAULT random_uuid() NOT NULL,
    user_id      varchar(64) NOT NULL,
    offer_id     uuid NOT NULL REFERENCES travel_offer(id),
    final_price  numeric(15,2) NOT NULL,
    booked_at    timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);
