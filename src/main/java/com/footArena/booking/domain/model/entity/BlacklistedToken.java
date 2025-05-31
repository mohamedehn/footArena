package com.footArena.booking.domain.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "token", length = 500)
    private String token;

    @Column(nullable = false, name = "blacklisted_at")
    private java.time.LocalDateTime blacklistedAt;

    @Column(nullable = false, name = "expires_at")
    private java.time.LocalDateTime expiresAt;

    public BlacklistedToken() {
    }

    public BlacklistedToken(String token, java.time.LocalDateTime blacklistedAt, java.time.LocalDateTime expiresAt) {
        this.token = token;
        this.blacklistedAt = blacklistedAt;
        this.expiresAt = expiresAt;
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

    public java.time.LocalDateTime getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(java.time.LocalDateTime blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }

    public java.time.LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(java.time.LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

}