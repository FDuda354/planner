ALTER TABLE transactions
    DROP CONSTRAINT transactions_shared_only_for_expenses,
    ADD CONSTRAINT transactions_shared_not_for_savings CHECK (shared = FALSE OR type <> 'SAVING');

ALTER TABLE recurring_transactions
    DROP CONSTRAINT recurring_transactions_shared_only_for_expenses,
    ADD CONSTRAINT recurring_transactions_shared_not_for_savings CHECK (shared = FALSE OR type <> 'SAVING');
