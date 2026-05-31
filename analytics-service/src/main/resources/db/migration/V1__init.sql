-- Staging: one row per consumed TransactionCategorized event
CREATE TABLE txn_fact (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  uuid        NOT NULL UNIQUE,
    user_id         varchar(64) NOT NULL,
    account_id      varchar(64) NOT NULL,
    amount          numeric(15,2) NOT NULL,
    mcc             varchar(8),
    category_name   varchar(128) NOT NULL,
    occurred_at     timestamptz NOT NULL,
    ingested_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_txn_fact_user_period ON txn_fact(user_id, occurred_at);

-- Per-user per-period rollup (period = YYYY-MM)
CREATE TABLE user_metrics (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             varchar(64) NOT NULL,
    period              varchar(7)  NOT NULL,
    income_estimate     numeric(15,2),
    total_spent         numeric(15,2) NOT NULL,
    avg_transaction     numeric(15,2),
    median_transaction  numeric(15,2),
    volatility          numeric(6,4),
    savings_rate        numeric(6,4),
    computed_at         timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, period)
);

-- Per-user-per-category-per-period
CREATE TABLE category_stat (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         varchar(64) NOT NULL,
    period          varchar(7)  NOT NULL,
    category_name   varchar(128) NOT NULL,
    txn_count       int NOT NULL,
    total_amount    numeric(15,2) NOT NULL,
    avg_amount      numeric(15,2) NOT NULL,
    median_amount   numeric(15,2) NOT NULL,
    UNIQUE (user_id, period, category_name)
);

-- Detected recurring debits (subscriptions, utilities)
CREATE TABLE recurring_debit (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         varchar(64) NOT NULL,
    label           varchar(128) NOT NULL,
    amount          numeric(15,2) NOT NULL,
    day_of_month    int,
    confidence      numeric(4,3) NOT NULL,
    last_seen_at    timestamptz NOT NULL,
    UNIQUE (user_id, label)
);

-- Plan-vs-actual drift snapshot
CREATE TABLE drift_report (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             varchar(64) NOT NULL,
    plan_id             uuid NOT NULL,
    period              varchar(7) NOT NULL,
    matches             boolean NOT NULL,
    drift_by_category   jsonb NOT NULL,
    recommend_adjustment boolean NOT NULL,
    computed_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_drift_user_period ON drift_report(user_id, period);
