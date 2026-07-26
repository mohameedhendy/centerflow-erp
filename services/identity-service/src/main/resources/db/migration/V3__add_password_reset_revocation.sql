ALTER TABLE password_reset_tokens
    ADD COLUMN revoked_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_password_reset_tokens_user_state
    ON password_reset_tokens (
                              user_id,
                              used_at,
                              revoked_at
        );