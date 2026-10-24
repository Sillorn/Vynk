package com.vynk.shared.error;

/**
 * A single field-level validation failure carried by an RFC 9457 problem response.
 *
 * @param field request field or parameter name, or {@code _request} for a request-wide failure
 * @param message human-readable, safe-to-expose validation message
 */
public record FieldViolation(String field, String message) {

    public FieldViolation {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
