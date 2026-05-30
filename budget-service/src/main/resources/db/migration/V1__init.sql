CREATE TABLE budget_plan (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id     varchar(255) NOT NULL,
    owner_type   varchar(16)  NOT NULL,
    period_start date         NOT NULL,
    period_end   date         NOT NULL,
    created_at   timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE budget_category (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id      uuid           NOT NULL REFERENCES budget_plan (id) ON DELETE CASCADE,
    name         varchar(64)    NOT NULL,
    type         varchar(16)    NOT NULL,
    limit_amount numeric(15, 2) NOT NULL,
    spent_amount numeric(15, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_budget_plan_owner ON budget_plan (owner_id, owner_type);
CREATE INDEX idx_budget_category_plan ON budget_category (plan_id);