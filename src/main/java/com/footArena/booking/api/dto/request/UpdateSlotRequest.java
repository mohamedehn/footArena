package com.footArena.booking.api.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpdateSlotRequest {

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal price;

    @Min(value = 1, message = "Max capacity must be at least 1")
    @Max(value = 50, message = "Max capacity cannot exceed 50")
    private Integer maxCapacity;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean isPremium;

    @Min(value = 1, message = "Cancellation deadline must be at least 1 hour")
    @Max(value = 168, message = "Cancellation deadline cannot exceed 168 hours")
    private Integer cancellationDeadlineHours;

    public UpdateSlotRequest() {
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(Boolean isPremium) {
        this.isPremium = isPremium;
    }

    public Integer getCancellationDeadlineHours() {
        return cancellationDeadlineHours;
    }

    public void setCancellationDeadlineHours(Integer cancellationDeadlineHours) {
        this.cancellationDeadlineHours = cancellationDeadlineHours;
    }
}