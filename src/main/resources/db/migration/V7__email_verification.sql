ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;
UPDATE users SET email_verified = TRUE WHERE email_verified IS NULL OR email_verified = FALSE;

ALTER TABLE handymen ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;
UPDATE handymen SET email_verified = TRUE WHERE email_verified IS NULL OR email_verified = FALSE;

ALTER TABLE company_registration_requests ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(100) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(40) NOT NULL,
    reference_id UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_token ON email_verification_tokens(token);
