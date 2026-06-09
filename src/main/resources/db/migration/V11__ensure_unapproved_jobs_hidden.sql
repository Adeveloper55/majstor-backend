-- Ponovo: OPEN bez token cene nije admin-odobren oglas
UPDATE job_listings
SET status = 'PENDING_APPROVAL'
WHERE status = 'OPEN'
  AND (token_cost IS NULL OR token_cost <= 0);
