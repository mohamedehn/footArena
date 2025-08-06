package com.footArena.booking.domain.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "token", length = Integer.MAX_VALUE)
    private String token;

    @Column(nullable = false, name = "blacklisted_at")
    private LocalDateTime blacklistedAt;

    @Column(nullable = false, name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false, name = "user_id")
    private UUID userId;

    @Column(nullable = false, name = "isBlackListed")
    private boolean isBlackListed;

    public BlacklistedToken() {
    }

    public BlacklistedToken(String token, LocalDateTime blacklistedAt, Instant expiresAt, UUID userId, boolean isBlackListed) {
        this.token = token;
        this.blacklistedAt = blacklistedAt;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.isBlackListed = isBlackListed;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(LocalDateTime blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean isBlackListed() {
        return isBlackListed;
    }

    public void setBlackListed(boolean isBlackListed) {
        this.isBlackListed = isBlackListed;
    }

}