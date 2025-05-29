package com.footArena.booking.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class InvoiceDTO {

    private UUID id;
    private String paymentMethod;
    private String status;
    private String stripeSessionId;
    private double amount;
    private UUID userId;
    private UUID matchId;
    private LocalDateTime issuedAt;

    public InvoiceDTO() {
    }

    public InvoiceDTO(UUID id, String paymentMethod, String status, String stripeSessionId, double amount, UUID userId, UUID matchId, LocalDateTime issuedAt) {
        this.id = id;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.stripeSessionId = stripeSessionId;
        this.amount = amount;
        this.userId = userId;
        this.matchId = matchId;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

}