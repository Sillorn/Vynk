/**
 * Persistent in-app notifications.
 *
 * <p>Owns notification records, read state and recipient-scoped queries. Delivery over SSE is
 * coordinated with the shared event infrastructure; notification content is always fetched
 * through authorized REST reads.</p>
 */
package com.vynk.notifications;
