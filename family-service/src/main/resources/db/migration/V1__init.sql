CREATE TABLE family_group (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       varchar(255) NOT NULL,
    type       varchar(32)  NOT NULL DEFAULT 'FAMILY',
    created_by varchar(255) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE membership (
    id       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id uuid        NOT NULL REFERENCES family_group (id) ON DELETE CASCADE,
    user_id  varchar(255) NOT NULL,
    role     varchar(16)  NOT NULL,
    CONSTRAINT uq_membership_group_user UNIQUE (group_id, user_id)
);

CREATE TABLE child_limit (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    membership_id  uuid           NOT NULL UNIQUE REFERENCES membership (id) ON DELETE CASCADE,
    daily_limit    numeric(15, 2) NOT NULL,
    override_until timestamptz,
    override_amount numeric(15, 2)
);

CREATE INDEX idx_membership_user ON membership (user_id);
CREATE INDEX idx_membership_group ON membership (group_id);
