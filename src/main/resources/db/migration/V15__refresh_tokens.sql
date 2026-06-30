CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    user_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_email ON refresh_tokens(email);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
