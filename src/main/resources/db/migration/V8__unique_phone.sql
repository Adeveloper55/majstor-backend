ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_normalized VARCHAR(30);
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS phone_normalized VARCHAR(30);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone_normalized
    ON users (phone_normalized) WHERE phone_normalized IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_handymen_phone_normalized
    ON handymen (phone_normalized) WHERE phone_normalized IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_company_reg_phone_pending
    ON company_registration_requests (normalized_phone)
    WHERE status = 'PENDING' AND normalized_phone IS NOT NULL;
