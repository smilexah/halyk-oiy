CREATE TABLE device_offer (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sku            varchar(64) NOT NULL UNIQUE,
    name           varchar(255) NOT NULL,
    base_price     numeric(15,2) NOT NULL,
    discount_pct   numeric(5,2) NOT NULL,
    audience_tags  jsonb NOT NULL,
    valid_until    timestamptz NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_alser_offer_audience ON device_offer USING GIN (audience_tags);

CREATE TABLE applied_discount (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      varchar(64) NOT NULL,
    offer_id     uuid NOT NULL REFERENCES device_offer(id),
    final_price  numeric(15,2) NOT NULL,
    applied_at   timestamptz NOT NULL DEFAULT now()
);

INSERT INTO device_offer (sku, name, base_price, discount_pct, audience_tags, valid_until) VALUES
    ('IPHONE-15-PRO', 'iPhone 15 Pro 256GB', 750000, 10.00, '["electronics-saver","apple-fan","high-income"]', now() + interval '30 days'),
    ('MACBOOK-AIR-M3', 'MacBook Air M3 13"',   850000, 12.00, '["electronics-saver","student","creator"]',     now() + interval '30 days'),
    ('AIRPODS-PRO-2',  'AirPods Pro (2nd gen)', 165000,  8.00, '["apple-fan","casual"]',                       now() + interval '30 days'),
    ('SAMSUNG-S24',    'Samsung Galaxy S24',    420000, 15.00, '["electronics-saver","android-fan"]',          now() + interval '30 days'),
    ('LG-OLED-55',     'LG OLED 55" C3',        780000, 10.00, '["home-upgrade","high-income"]',               now() + interval '30 days'),
    ('SONY-WH-1000XM5','Sony WH-1000XM5',       220000, 12.00, '["commuter","music-lover"]',                   now() + interval '30 days'),
    ('DJI-MINI-4',     'DJI Mini 4 Pro',        650000,  5.00, '["creator","traveler"]',                       now() + interval '30 days'),
    ('PS5-SLIM',       'PlayStation 5 Slim',    310000, 10.00, '["gamer","student"]',                          now() + interval '30 days'),
    ('XBOX-X',         'Xbox Series X',         320000, 10.00, '["gamer"]',                                    now() + interval '30 days'),
    ('IPAD-AIR-M2',    'iPad Air M2 11"',       420000, 10.00, '["student","creator","apple-fan"]',            now() + interval '30 days');
