CREATE TABLE travel_offer (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    kind           varchar(16) NOT NULL CHECK (kind IN ('FLIGHT','HOTEL','TOUR')),
    destination    varchar(128) NOT NULL,
    base_price     numeric(15,2) NOT NULL,
    discount_pct   numeric(5,2) NOT NULL,
    audience_tags  jsonb NOT NULL,
    valid_until    timestamptz NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_travel_offer_audience ON travel_offer USING GIN (audience_tags);

CREATE TABLE booking (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      varchar(64) NOT NULL,
    offer_id     uuid NOT NULL REFERENCES travel_offer(id),
    final_price  numeric(15,2) NOT NULL,
    booked_at    timestamptz NOT NULL DEFAULT now()
);

INSERT INTO travel_offer (kind, destination, base_price, discount_pct, audience_tags, valid_until) VALUES
    ('FLIGHT', 'Astana → Antalya',         180000, 15.00, '["saving_for_trip","beach-lover","summer"]',    now() + interval '60 days'),
    ('HOTEL',  'Dubai 5★ — 4 nights',      420000, 20.00, '["saving_for_trip","luxury","high-income"]',    now() + interval '60 days'),
    ('TOUR',   'Bali full package 10d',    750000, 10.00, '["saving_for_trip","explorer"]',                now() + interval '90 days'),
    ('FLIGHT', 'Astana → Tbilisi',          95000, 10.00, '["budget-traveler","weekend"]',                 now() + interval '60 days'),
    ('HOTEL',  'Issyk-Kul resort — 3 d',    55000, 15.00, '["family-with-kids","local-getaway"]',          now() + interval '60 days'),
    ('TOUR',   'Almaty mountains 2 days',   35000,  8.00, '["budget-traveler","local-getaway","outdoor"]', now() + interval '60 days'),
    ('FLIGHT', 'Astana → Istanbul',        145000, 12.00, '["explorer","budget-traveler"]',                now() + interval '60 days'),
    ('HOTEL',  'Borovoye — 2 nights',       45000, 10.00, '["family-with-kids","local-getaway"]',          now() + interval '60 days'),
    ('TOUR',   'Umrah package 7 days',     650000,  5.00, '["religious","high-income"]',                   now() + interval '90 days'),
    ('FLIGHT', 'Astana → Bangkok',         280000, 10.00, '["explorer","summer","saving_for_trip"]',       now() + interval '60 days');
