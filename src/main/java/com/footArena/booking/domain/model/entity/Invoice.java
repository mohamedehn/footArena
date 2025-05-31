package com.footArena.booking.domain.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "stripe_session_id", nullable = false)
    private String stripeSessionId;

    @Column(name = "amount", nullable = false)
    private double amount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    private java.time.LocalDateTime issuedAt;

    public Invoice() {
    }

    public Invoice(String paymentMethod, String status, String stripeSessionId, double amount, User user, Match match, java.time.LocalDateTime issuedAt) {
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.stripeSessionId = stripeSessionId;
        this.amount = amount;
        this.user = user;
        this.match = match;
        this.issuedAt = issuedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public java.time.LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(java.time.LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

}