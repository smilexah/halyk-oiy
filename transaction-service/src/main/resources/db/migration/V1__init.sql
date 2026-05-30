CREATE TABLE txn (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    varchar(255)   NOT NULL,
    user_id       varchar(255)   NOT NULL,
    amount        numeric(15, 2) NOT NULL,
    merchant      varchar(255),
    mcc           varchar(8),
    category_name varchar(64),
    occurred_at   timestamptz    NOT NULL,
    status        varchar(32)    NOT NULL
);

CREATE INDEX idx_txn_user_id ON txn (user_id);
CREATE INDEX idx_txn_occurred_at ON txn (occurred_at);