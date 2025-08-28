ALTER TABLE users DROP COLUMN providerId;
ALTER TABLE users ADD provider_id VARCHAR(255);