ALTER TABLE token_purchase_requests
    ADD COLUMN IF NOT EXISTS predracun_sent_at TIMESTAMP;
