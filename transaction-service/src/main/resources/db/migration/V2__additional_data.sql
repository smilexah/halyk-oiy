ALTER TABLE txn
    ADD COLUMN direction       VARCHAR(8)     NOT NULL DEFAULT 'DEBIT'
        CHECK (direction IN ('DEBIT', 'CREDIT')),
    ADD COLUMN operation_type  VARCHAR(16)    NOT NULL DEFAULT 'PURCHASE'
        CHECK (operation_type IN ('PURCHASE', 'TRANSFER', 'TOPUP', 'PAYMENT', 'SALARY', 'MISC')),
    ADD COLUMN currency        VARCHAR(3)     NOT NULL DEFAULT 'KZT',
    ADD COLUMN details         VARCHAR(512),
    ADD COLUMN balance_after   NUMERIC(15, 2);

-- Composite index for analytics-service queries that filter by user and time window.
-- V1 already has individual idx_txn_user_id and idx_txn_occurred_at; this composite
-- one allows efficient range scans per user without a separate index merge step.
CREATE INDEX idx_txn_user_period ON txn (user_id, occurred_at);
