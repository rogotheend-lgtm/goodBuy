CREATE TABLE anonymous_session (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    last_accessed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_anonymous_session_expires_at
    ON anonymous_session (expires_at);

CREATE TABLE analysis (
    id UUID PRIMARY KEY,
    anonymous_session_id UUID NOT NULL REFERENCES anonymous_session (id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_analysis_session_created_at
    ON analysis (anonymous_session_id, created_at DESC);

CREATE TABLE expense_transaction (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES analysis (id) ON DELETE CASCADE,
    item_order INTEGER NOT NULL,
    counterparty VARCHAR(100) NOT NULL,
    original_amount BIGINT NOT NULL CHECK (original_amount > 0),
    personal_amount BIGINT NOT NULL CHECK (personal_amount >= 0 AND personal_amount <= original_amount),
    transaction_type VARCHAR(30) NOT NULL,
    purpose_category VARCHAR(30) NOT NULL,
    merchant_type VARCHAR(30) NOT NULL,
    decision_source VARCHAR(20) NOT NULL,
    requires_review BOOLEAN NOT NULL,
    review_reason VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (analysis_id, item_order)
);

CREATE INDEX idx_expense_transaction_analysis
    ON expense_transaction (analysis_id, item_order);
