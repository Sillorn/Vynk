# Message retry and notification policy

## Message send idempotency

`POST /api/v1/conversations/with/{username}/messages` requires a UUID `clientRequestId` in the JSON body. A client must generate it once per logical send and reuse it unchanged for retries. The server scopes keys to the authenticated sender and stores them with the message under a unique database constraint. Repeating the same key for the same conversation and normalized body returns the original message and does not emit another SSE event. Reusing a key for different content or a different recipient returns a conflict. Clients must use a new UUID for a new logical message.

## Notification policy

Persistent notifications are for connection lifecycle events only; message content and message-send notifications are not persisted. The recipient of a connection request may receive a `connection_request` notification. The requester may receive a `connection_accepted` notification when accepted. Cancel, decline, disconnect, and message events do not create persistent notifications. Real-time message and unread changes use event-type-only SSE signals; clients refresh authorized REST resources for data.
