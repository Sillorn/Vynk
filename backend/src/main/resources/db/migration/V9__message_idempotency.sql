ALTER TABLE conversation_message ADD COLUMN client_request_id UUID;

-- Existing rows predate client-side idempotency keys; assign unique migration values.
UPDATE conversation_message SET client_request_id = RANDOM_UUID();
ALTER TABLE conversation_message ALTER COLUMN client_request_id SET NOT NULL;
CREATE UNIQUE INDEX uq_message_sender_client_request
    ON conversation_message (sender_id, client_request_id);
