package com.vynk.notifications.entities;

import com.vynk.identity.entities.UserAccount;
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

@Entity
@Table(name = "notification")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recipient_id", nullable = false) private UserAccount recipient;
    @Column(nullable = false, length = 40) private String type;
    @Column(name = "actor_username", length = 30) private String actorUsername;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "read_at") private Instant readAt;
    protected Notification() {}
    public Notification(UserAccount recipient, String type, String actorUsername) {
        this.recipient = recipient; this.type = type; this.actorUsername = actorUsername;
    }
    public Long getId() { return id; }
    public String getType() { return type; }
    public String getActorUsername() { return actorUsername; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead() { if (readAt == null) readAt = Instant.now(); }
}
