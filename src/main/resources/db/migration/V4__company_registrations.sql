-- Registracije preduzeća (čekaju admin odobrenje)
CREATE TABLE company_registration_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    normalized_phone VARCHAR(30) NOT NULL,
    selected_service_ids TEXT NOT NULL DEFAULT '[]',
    selected_service_names TEXT NOT NULL DEFAULT '[]',
    company_short_description VARCHAR(100),
    selected_districts TEXT NOT NULL DEFAULT '[]',
    company_name VARCHAR(200) NOT NULL,
    pib VARCHAR(9) NOT NULL,
    address TEXT NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(50) NOT NULL DEFAULT 'Srbija',
    contact_person VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note TEXT,
    handyman_id UUID REFERENCES handymen(id),
    created_at TIMESTAMP DEFAULT NOW(),
    reviewed_at TIMESTAMP
);

CREATE INDEX idx_company_reg_status ON company_registration_requests(status);
CREATE INDEX idx_company_reg_created ON company_registration_requests(created_at DESC);

-- Proširenje majstora za preduzeća
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS company_name VARCHAR(200);
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS pib VARCHAR(9);
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS country VARCHAR(50);
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS contact_person VARCHAR(100);
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS is_company BOOLEAN DEFAULT FALSE;
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS coverage_districts TEXT;
ALTER TABLE handymen ADD COLUMN IF NOT EXISTS service_categories TEXT;
