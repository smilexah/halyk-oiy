ALTER TABLE budget_plan
    ADD COLUMN version        int     NOT NULL DEFAULT 1,
    ADD COLUMN created_by_ai  boolean NOT NULL DEFAULT false,
    ADD COLUMN superseded_by  uuid    NULL REFERENCES budget_plan(id);

CREATE INDEX idx_budget_plan_active
    ON budget_plan (owner_id, owner_type)
    WHERE superseded_by IS NULL;
