package com.footArena.booking.domain.entities;

import com.footArena.booking.domain.enums.SlotStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "slots")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private Field field;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "current_bookings", nullable = false)
    private Integer currentBookings = 0;

    @Column(name = "description")
    private String description;

    @Column(name = "recurring_pattern")
    private String recurringPattern; // Pour les créneaux récurrents

    @Column(name = "is_premium", nullable = false)
    private Boolean isPremium = false;

    @Column(name = "cancellation_deadline_hours", nullable = false)
    private Integer cancellationDeadlineHours = 24; // Délai d'annulation en heures

    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Slot() {
    }

    public Slot(Field field, LocalDateTime startTime, LocalDateTime endTime,
                BigDecimal price, Integer maxCapacity) {
        this.field = field;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.maxCapacity = maxCapacity;
        this.status = SlotStatus.AVAILABLE;
        this.currentBookings = 0;
        this.isPremium = false;
    }

    public boolean isAvailable() {
        return status == SlotStatus.AVAILABLE && currentBookings < maxCapacity;
    }

    public boolean isFull() {
        return currentBookings >= maxCapacity;
    }

    public boolean canBeCancelled() {
        return LocalDateTime.now().isBefore(startTime.minusHours(cancellationDeadlineHours));
    }

    public boolean isInPast() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public boolean isToday() {
        LocalDateTime now = LocalDateTime.now();
        return startTime.toLocalDate().equals(now.toLocalDate());
    }

    public long getDurationInMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    public int getAvailableSpots() {
        return maxCapacity - currentBookings;
    }

    public void incrementBookings() {
        this.currentBookings++;
        updateStatus();
    }

    public void decrementBookings() {
        if (this.currentBookings > 0) {
            this.currentBookings--;
            updateStatus();
        }
    }

    private void updateStatus() {
        if (currentBookings >= maxCapacity) {
            this.status = SlotStatus.FULL;
        } else if (currentBookings > 0) {
            this.status = SlotStatus.RESERVED;
        } else {
            this.status = SlotStatus.AVAILABLE;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
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

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getCurrentBookings() {
        return currentBookings;
    }

    public void setCurrentBookings(Integer currentBookings) {
        this.currentBookings = currentBookings;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRecurringPattern() {
        return recurringPattern;
    }

    public void setRecurringPattern(String recurringPattern) {
        this.recurringPattern = recurringPattern;
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

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}