CREATE TABLE recommendation (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       varchar(64) NOT NULL,
    period        varchar(7)  NOT NULL,
    offer_id      varchar(64) NOT NULL,
    partner       varchar(32) NOT NULL,
    score         numeric(4,3) NOT NULL,
    audience_tags jsonb       NOT NULL,
    rationale     text,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_recommendation_user_period ON recommendation (user_id, period);
