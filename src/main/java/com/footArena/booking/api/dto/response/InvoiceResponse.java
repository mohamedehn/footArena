package com.footArena.booking.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class InvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private BigDecimal amountHT;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal amountTTC;
    private String status;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private String description;
    private String customerName;
    private String customerEmail;
    private String customerAddress;
    private String establishmentName;
    private String establishmentAddress;
    private String establishmentSiret;
    private String invoiceFilePath;
    private LocalDateTime issuedAt;
    private Boolean isPaid;
    private Boolean isCancelled;
    private Boolean isOverdue;
    private PaymentResponse payment;

    public InvoiceResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public BigDecimal getAmountHT() {
        return amountHT;
    }

    public void setAmountHT(BigDecimal amountHT) {
        this.amountHT = amountHT;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getAmountTTC() {
        return amountTTC;
    }

    public void setAmountTTC(BigDecimal amountTTC) {
        this.amountTTC = amountTTC;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getEstablishmentName() {
        return establishmentName;
    }

    public void setEstablishmentName(String establishmentName) {
        this.establishmentName = establishmentName;
    }

    public String getEstablishmentAddress() {
        return establishmentAddress;
    }

    public void setEstablishmentAddress(String establishmentAddress) {
        this.establishmentAddress = establishmentAddress;
    }

    public String getEstablishmentSiret() {
        return establishmentSiret;
    }

    public void setEstablishmentSiret(String establishmentSiret) {
        this.establishmentSiret = establishmentSiret;
    }

    public String getInvoiceFilePath() {
        return invoiceFilePath;
    }

    public void setInvoiceFilePath(String invoiceFilePath) {
        this.invoiceFilePath = invoiceFilePath;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }

    public Boolean getIsCancelled() {
        return isCancelled;
    }

    public void setIsCancelled(Boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public Boolean getIsOverdue() {
        return isOverdue;
    }

    public void setIsOverdue(Boolean isOverdue) {
        this.isOverdue = isOverdue;
    }

    public PaymentResponse getPayment() {
        return payment;
    }

    public void setPayment(PaymentResponse payment) {
        this.payment = payment;
    }

    // Méthodes utilitaires
    public String getStatusDisplay() {
        switch (status) {
            case "ISSUED":
                return "Émise";
            case "PAID":
                return "Payée";
            case "CANCELLED":
                return "Annulée";
            default:
                return status;
        }
    }

    public String getFormattedAmountTTC() {
        return String.format("%.2f €", amountTTC);
    }

    public String getFormattedAmountHT() {
        return String.format("%.2f €", amountHT);
    }

    public String getFormattedTaxAmount() {
        return String.format("%.2f €", taxAmount);
    }
}