package com.vynk.messaging.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "conversation")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_low_id", nullable = false, updatable = false)
    private Long userLowId;

    @Column(name = "user_high_id", nullable = false, updatable = false)
    private Long userHighId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Conversation() {}

    public Conversation(long firstUserId, long secondUserId) {
        if (firstUserId == secondUserId) {
            throw new IllegalArgumentException("A conversation requires two different users");
        }
        userLowId = Math.min(firstUserId, secondUserId);
        userHighId = Math.max(firstUserId, secondUserId);
    }

    public Long getId() { return id; }
    public Long getUserLowId() { return userLowId; }
    public Long getUserHighId() { return userHighId; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean hasParticipant(long userId) { return userLowId == userId || userHighId == userId; }
}
