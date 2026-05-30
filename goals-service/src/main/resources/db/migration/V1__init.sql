CREATE TABLE virtual_account (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id   varchar(255)   NOT NULL,
    owner_type varchar(16)    NOT NULL,
    balance    numeric(15, 2) NOT NULL DEFAULT 0
);

CREATE TABLE goal (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 varchar(255)   NOT NULL,
    description          varchar(1024),
    category             varchar(64),
    target_amount        numeric(15, 2) NOT NULL,
    allocated_amount     numeric(15, 2) NOT NULL DEFAULT 0,
    monthly_contribution numeric(15, 2),
    deadline             date,
    virtual_account_id   uuid           NOT NULL REFERENCES virtual_account (id),
    bonus_program_ref    varchar(255),
    created_at           timestamptz    NOT NULL DEFAULT now()
);

CREATE INDEX idx_virtual_account_owner ON virtual_account (owner_id);
CREATE INDEX idx_goal_account ON goal (virtual_account_id);