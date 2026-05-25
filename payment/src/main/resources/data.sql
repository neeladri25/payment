-- Insert Payment Provider Configurations
INSERT INTO payment_providers (name, supported_method, api_endpoint, is_active) VALUES
('Provider A', 'CARD', 'https://api.provider-a.com/payment', TRUE),
('Provider B', 'UPI', 'https://api.provider-b.com/payment', TRUE),
('Provider C', 'WALLET', 'https://api.provider-c.com/payment', TRUE),
('Provider D', 'BANK_TRANSFER', 'https://api.provider-d.com/payment', TRUE);

