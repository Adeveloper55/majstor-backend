-- Unique PIB per majstor (when provided)
CREATE UNIQUE INDEX IF NOT EXISTS idx_handymen_pib_unique ON handymen (pib) WHERE pib IS NOT NULL;
