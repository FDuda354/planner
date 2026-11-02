ALTER TABLE recurring_transactions
    ADD COLUMN frequency     VARCHAR(10) NOT NULL DEFAULT 'MONTHLY' CHECK (frequency IN ('MONTHLY', 'YEARLY')),
    ADD COLUMN month_of_year INTEGER CHECK (month_of_year BETWEEN 1 AND 12),
    ADD CONSTRAINT recurring_transactions_month_matches_frequency CHECK (
        (frequency = 'MONTHLY' AND month_of_year IS NULL)
        OR (frequency = 'YEARLY' AND month_of_year IS NOT NULL)
    );
