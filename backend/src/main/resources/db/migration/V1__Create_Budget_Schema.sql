CREATE SEQUENCE household_id_sequence INCREMENT 1;
CREATE SEQUENCE member_id_sequence INCREMENT 1;
CREATE SEQUENCE invitation_id_sequence INCREMENT 1;
CREATE SEQUENCE expense_category_id_sequence INCREMENT 1;
CREATE SEQUENCE income_source_id_sequence INCREMENT 1;
CREATE SEQUENCE savings_goal_id_sequence INCREMENT 1;
CREATE SEQUENCE transaction_id_sequence INCREMENT 1;

CREATE TABLE households
(
    id         BIGINT       DEFAULT nextval('household_id_sequence') PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE members
(
    id           BIGINT       DEFAULT nextval('member_id_sequence') PRIMARY KEY,
    household_id BIGINT       NOT NULL REFERENCES households (id),
    first_name   VARCHAR(60)  NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    password     VARCHAR(100) NOT NULL,
    color        VARCHAR(7)   NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE invitations
(
    id           BIGINT       DEFAULT nextval('invitation_id_sequence') PRIMARY KEY,
    household_id BIGINT       NOT NULL REFERENCES households (id),
    email        VARCHAR(150) NOT NULL UNIQUE,
    invited_by   BIGINT       NOT NULL REFERENCES members (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE expense_categories
(
    id           BIGINT      DEFAULT nextval('expense_category_id_sequence') PRIMARY KEY,
    household_id BIGINT      NOT NULL REFERENCES households (id),
    name         VARCHAR(60) NOT NULL,
    icon         VARCHAR(40) NOT NULL,
    color        VARCHAR(7)  NOT NULL,
    active       BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE income_sources
(
    id           BIGINT      DEFAULT nextval('income_source_id_sequence') PRIMARY KEY,
    household_id BIGINT      NOT NULL REFERENCES households (id),
    name         VARCHAR(60) NOT NULL,
    icon         VARCHAR(40) NOT NULL,
    color        VARCHAR(7)  NOT NULL,
    active       BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE savings_goals
(
    id             BIGINT         DEFAULT nextval('savings_goal_id_sequence') PRIMARY KEY,
    household_id   BIGINT         NOT NULL REFERENCES households (id),
    name           VARCHAR(60)    NOT NULL,
    icon           VARCHAR(40)    NOT NULL,
    color          VARCHAR(7)     NOT NULL,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    note           VARCHAR(255)   NOT NULL DEFAULT '',
    initial_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (initial_amount >= 0),
    target_amount  NUMERIC(12, 2) NOT NULL CHECK (target_amount > 0),
    owner_id       BIGINT REFERENCES members (id)
);

CREATE TABLE transactions
(
    id                  BIGINT         DEFAULT nextval('transaction_id_sequence') PRIMARY KEY,
    household_id        BIGINT         NOT NULL REFERENCES households (id),
    type                VARCHAR(10)    NOT NULL CHECK (type IN ('EXPENSE', 'INCOME', 'SAVING')),
    expense_category_id BIGINT REFERENCES expense_categories (id),
    income_source_id    BIGINT REFERENCES income_sources (id),
    savings_goal_id     BIGINT REFERENCES savings_goals (id),
    amount              NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    transaction_date    DATE           NOT NULL,
    member_id           BIGINT         NOT NULL REFERENCES members (id),
    shared              BOOLEAN        NOT NULL DEFAULT FALSE,
    note                VARCHAR(255)   NOT NULL DEFAULT '',
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT transactions_item_matches_type CHECK (
        (type = 'EXPENSE' AND expense_category_id IS NOT NULL AND income_source_id IS NULL AND savings_goal_id IS NULL)
        OR (type = 'INCOME' AND income_source_id IS NOT NULL AND expense_category_id IS NULL AND savings_goal_id IS NULL)
        OR (type = 'SAVING' AND savings_goal_id IS NOT NULL AND expense_category_id IS NULL AND income_source_id IS NULL)
    ),
    CONSTRAINT transactions_shared_only_for_expenses CHECK (shared = FALSE OR type = 'EXPENSE')
);

CREATE INDEX idx_members_household_id ON members (household_id);
CREATE INDEX idx_invitations_household_id ON invitations (household_id);
CREATE INDEX idx_expense_categories_household_id ON expense_categories (household_id);
CREATE INDEX idx_income_sources_household_id ON income_sources (household_id);
CREATE INDEX idx_savings_goals_household_id ON savings_goals (household_id);
CREATE INDEX idx_transactions_household_date ON transactions (household_id, transaction_date);
CREATE INDEX idx_transactions_expense_category_id ON transactions (expense_category_id);
CREATE INDEX idx_transactions_income_source_id ON transactions (income_source_id);
CREATE INDEX idx_transactions_savings_goal_id ON transactions (savings_goal_id);
CREATE INDEX idx_transactions_member_id ON transactions (member_id);
