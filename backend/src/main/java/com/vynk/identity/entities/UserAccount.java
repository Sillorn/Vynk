package com.vynk.identity.entities;

import com.vynk.identity.UserAccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String username;

    @Column(name = "username_lower", nullable = false, length = 30)
    private String usernameLower;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserAccountStatus status = UserAccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToOne(mappedBy = "userAccount")
    private Profile profile;

    protected UserAccount() {
    }

    public UserAccount(String username, String passwordHash) {
        this.username = username;
        this.usernameLower = username == null ? null : username.toLowerCase();
        this.passwordHash = passwordHash;
        this.status = UserAccountStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    private void normalizeUsername() {
        if (username != null) {
            this.usernameLower = username.toLowerCase();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserAccountStatus getStatus() {
        return status;
    }

    public void setStatus(UserAccountStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
