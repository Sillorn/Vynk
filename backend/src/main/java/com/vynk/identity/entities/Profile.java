package com.vynk.identity.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "profile")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name_lower", length = 100)
    private String firstNameLower;

    @Column(name = "last_name_lower", length = 100)
    private String lastNameLower;

    @Column(nullable = false, length = 200)
    private String headline;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(length = 200)
    private String location;

    @Lob
    @Column(name = "links_json")
    private String linksJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Profile() {
    }

    public Profile(UserAccount userAccount, String firstName, String lastName, String headline, String summary) {
        this.userAccount = userAccount;
        this.firstName = firstName;
        this.lastName = lastName;
        this.firstNameLower = firstName == null ? null : firstName.toLowerCase();
        this.lastNameLower = lastName == null ? null : lastName.toLowerCase();
        this.headline = headline;
        this.summary = summary;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    private void normalizeNames() {
        if (firstName != null) {
            this.firstNameLower = firstName.toLowerCase();
        }
        if (lastName != null) {
            this.lastNameLower = lastName.toLowerCase();
        }
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.updatedAt = Instant.now();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.updatedAt = Instant.now();
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
        this.updatedAt = Instant.now();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
        this.updatedAt = Instant.now();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        this.updatedAt = Instant.now();
    }

    public String getLinksJson() {
        return linksJson;
    }

    public void setLinksJson(String linksJson) {
        this.linksJson = linksJson;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
