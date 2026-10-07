ALTER TABLE kfc_user
    ADD COLUMN phone_plain VARCHAR(32) NULL,
    ADD COLUMN phone_ciphertext VARCHAR(512) NULL,
    ADD COLUMN token_plain TEXT NULL,
    ADD COLUMN token_ciphertext TEXT NULL,
    ADD COLUMN token_updated_at DATETIME(3) NULL;
