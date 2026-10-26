-- Canonical accepted connection pairs. Request lifecycle is added by the connections feature.
CREATE TABLE accepted_connection (
    user_low_id BIGINT NOT NULL,
    user_high_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_accepted_connection PRIMARY KEY (user_low_id, user_high_id),
    CONSTRAINT fk_connection_low FOREIGN KEY (user_low_id) REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_connection_high FOREIGN KEY (user_high_id) REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT chk_connection_order CHECK (user_low_id < user_high_id)
);
CREATE INDEX idx_connection_high ON accepted_connection (user_high_id, user_low_id);
