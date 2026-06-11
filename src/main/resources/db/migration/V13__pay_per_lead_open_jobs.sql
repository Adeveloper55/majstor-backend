-- Pay-per-lead: poslovi odmah OPEN sa token_cost iz AI ocene
UPDATE job_listings j
SET status = 'OPEN',
    token_cost = GREATEST(1, COALESCE(j.ai_score, 1) * (
        SELECT COALESCE(c.base_token_cost, 1) FROM categories c WHERE c.id = j.category_id
    ))
WHERE status = 'PENDING_APPROVAL'
  AND (token_cost IS NULL OR token_cost <= 0);

UPDATE job_listings j
SET token_cost = GREATEST(1, COALESCE(j.ai_score, 1) * (
        SELECT COALESCE(c.base_token_cost, 1) FROM categories c WHERE c.id = j.category_id
    ))
WHERE status = 'OPEN'
  AND (token_cost IS NULL OR token_cost <= 0);

UPDATE job_applications SET status = 'UNLOCKED' WHERE status = 'ACCEPTED';
