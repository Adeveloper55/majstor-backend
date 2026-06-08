UPDATE token_packages SET is_active = FALSE;

INSERT INTO token_packages (name, token_amount, price_eur, is_active) VALUES
('Lajt paket', 100, 12000.00, TRUE),
('Minipaket', 200, 22000.00, TRUE),
('Clasic paket', 500, 46000.00, TRUE),
('Premium paket', 1000, 80000.00, TRUE);
