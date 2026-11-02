CREATE SEQUENCE recurring_transaction_id_sequence INCREMENT 1;

CREATE TABLE recurring_transactions
(
    id                  BIGINT         DEFAULT nextval('recurring_transaction_id_sequence') PRIMARY KEY,
    household_id        BIGINT         NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    type                VARCHAR(10)    NOT NULL CHECK (type IN ('EXPENSE', 'INCOME', 'SAVING')),
    expense_category_id BIGINT REFERENCES expense_categories (id) ON DELETE CASCADE,
    income_source_id    BIGINT REFERENCES income_sources (id) ON DELETE CASCADE,
    savings_goal_id     BIGINT REFERENCES savings_goals (id) ON DELETE CASCADE,
    amount              NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    day_of_month        INTEGER        NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    member_id           BIGINT         NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    shared              BOOLEAN        NOT NULL DEFAULT FALSE,
    note                VARCHAR(255)   NOT NULL DEFAULT '',
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    next_occurrence     DATE           NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT recurring_transactions_item_matches_type CHECK (
        (type = 'EXPENSE' AND expense_category_id IS NOT NULL AND income_source_id IS NULL AND savings_goal_id IS NULL)
        OR (type = 'INCOME' AND income_source_id IS NOT NULL AND expense_category_id IS NULL AND savings_goal_id IS NULL)
        OR (type = 'SAVING' AND savings_goal_id IS NOT NULL AND expense_category_id IS NULL AND income_source_id IS NULL)
    ),
    CONSTRAINT recurring_transactions_shared_only_for_expenses CHECK (shared = FALSE OR type = 'EXPENSE')
);

CREATE INDEX idx_recurring_transactions_household_id ON recurring_transactions (household_id);
CREATE INDEX idx_recurring_transactions_next_occurrence ON recurring_transactions (next_occurrence) WHERE active;

ALTER TABLE transactions
    ADD COLUMN recurring_transaction_id BIGINT REFERENCES recurring_transactions (id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_transactions_recurring_date ON transactions (recurring_transaction_id, transaction_date)
    WHERE recurring_transaction_id IS NOT NULL;
