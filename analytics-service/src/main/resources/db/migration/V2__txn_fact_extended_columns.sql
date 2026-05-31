-- Add extended columns to txn_fact to retain the full picture from TransactionCategorized events.
ALTER TABLE txn_fact
    ADD COLUMN IF NOT EXISTS direction       varchar(8),
    ADD COLUMN IF NOT EXISTS operation_type  varchar(16),
    ADD COLUMN IF NOT EXISTS currency        varchar(3)   NOT NULL DEFAULT 'KZT',
    ADD COLUMN IF NOT EXISTS details         varchar(512),
    ADD COLUMN IF NOT EXISTS balance_after   numeric(15,2);
