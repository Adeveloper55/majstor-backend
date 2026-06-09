-- Poslovi sa OPEN ali bez token cene nisu prošli admin odobrenje
UPDATE job_listings
SET status = 'PENDING_APPROVAL'
WHERE status = 'OPEN'
  AND (token_cost IS NULL OR token_cost <= 0);
