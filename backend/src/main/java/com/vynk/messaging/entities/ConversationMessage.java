package com.vynk.messaging.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_message")
public class ConversationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private Conversation conversation;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private Long senderId;

    @Column(name = "client_request_id", nullable = false, updatable = false)
    private UUID clientRequestId;

    @Column(nullable = false, length = 5000, updatable = false)
    private String body;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected ConversationMessage() {}

    public ConversationMessage(Conversation conversation, long senderId, String body, UUID clientRequestId) {
        this.conversation = conversation;
        this.senderId = senderId;
        this.body = body;
        this.clientRequestId = clientRequestId;
    }

    public Long getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public Long getSenderId() { return senderId; }
    public UUID getClientRequestId() { return clientRequestId; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
