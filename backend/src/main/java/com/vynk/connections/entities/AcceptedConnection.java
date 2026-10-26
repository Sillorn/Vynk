package com.vynk.connections.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "accepted_connection")
public class AcceptedConnection {
    @EmbeddedId
    private Key id;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected AcceptedConnection() {}

    public AcceptedConnection(long first, long second) {
        this.id = new Key(Math.min(first, second), Math.max(first, second));
    }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "user_low_id")
        private Long low;
        @Column(name = "user_high_id")
        private Long high;

        protected Key() {}
        public Key(Long low, Long high) { this.low = low; this.high = high; }
        public Long getLow() { return low; }
        public Long getHigh() { return high; }
        @Override public boolean equals(Object other) {
            return other instanceof Key key && low.equals(key.low) && high.equals(key.high);
        }
        @Override public int hashCode() { return java.util.Objects.hash(low, high); }
    }
}
