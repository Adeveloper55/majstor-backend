ALTER TABLE job_listings
    ADD COLUMN IF NOT EXISTS location_pinned BOOLEAN NOT NULL DEFAULT false;

UPDATE job_listings
SET location_pinned = false
WHERE location_pinned IS NULL;
