package com.vynk.connections.entities;

import com.vynk.connections.ConnectionRequestStatus;
import com.vynk.identity.entities.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "connection_request")
public class ConnectionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private UserAccount sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false, updatable = false)
    private UserAccount recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConnectionRequestStatus status = ConnectionRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ConnectionRequest() {}

    public ConnectionRequest(UserAccount sender, UserAccount recipient) {
        if (sender == null || recipient == null || sender.getId() == null || recipient.getId() == null
                || sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("A connection request requires two different saved accounts.");
        }
        this.sender = sender;
        this.recipient = recipient;
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public UserAccount getSender() { return sender; }
    public UserAccount getRecipient() { return recipient; }
    public ConnectionRequestStatus getStatus() { return status; }
    public void setStatus(ConnectionRequestStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
