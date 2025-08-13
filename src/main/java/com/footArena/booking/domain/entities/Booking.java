package com.footArena.booking.domain.entities;

import com.footArena.booking.domain.enums.BookingStatus;
import com.footArena.booking.domain.enums.BookingType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Utilisateur qui fait la réservation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "number_of_players", nullable = false)
    private Integer numberOfPlayers;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "special_requests", length = 500)
    private String specialRequests;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "booking_reference", unique = true)
    private String bookingReference; // Référence unique pour le client

    @Column(name = "confirmation_deadline")
    private LocalDateTime confirmationDeadline; // Délai pour confirmer la réservation

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "admin_notes")
    private String adminNotes; // Notes internes pour l'administration

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingPlayer> players = new ArrayList<>(); // Joueurs participants

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Booking() {
    }

    public Booking(User user, Slot slot, BookingType bookingType,
                   Integer numberOfPlayers, BigDecimal totalAmount) {
        this.user = user;
        this.slot = slot;
        this.bookingType = bookingType;
        this.numberOfPlayers = numberOfPlayers;
        this.totalAmount = totalAmount;
        this.status = BookingStatus.PENDING;
        this.isPaid = false;
        this.bookingReference = generateBookingReference();
        this.confirmationDeadline = LocalDateTime.now().plusHours(2); // 2h pour confirmer
    }

    public boolean canBeCancelled() {
        return !isInFinalState() && slot.canBeCancelled();
    }

    public boolean isInFinalState() {
        return status == BookingStatus.COMPLETED ||
                status == BookingStatus.CANCELLED ||
                status == BookingStatus.CANCELLED_BY_ESTABLISHMENT ||
                status == BookingStatus.EXPIRED;
    }

    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public boolean needsPayment() {
        return (status == BookingStatus.CONFIRMED || status == BookingStatus.AWAITING_PAYMENT)
                && !isPaid;
    }

    public boolean isExpired() {
        return status == BookingStatus.PENDING &&
                LocalDateTime.now().isAfter(confirmationDeadline);
    }

    public void confirm() {
        if (status == BookingStatus.PENDING) {
            this.status = BookingStatus.CONFIRMED;
            this.confirmedAt = LocalDateTime.now();
        }
    }

    public void cancel(String reason) {
        if (canBeCancelled()) {
            this.status = BookingStatus.CANCELLED;
            this.cancelledAt = LocalDateTime.now();
            this.cancellationReason = reason;
        }
    }

    public void complete() {
        if (status == BookingStatus.CONFIRMED && isPaid) {
            this.status = BookingStatus.COMPLETED;
        }
    }

    public void markAsPaid() {
        this.isPaid = true;
        if (status == BookingStatus.AWAITING_PAYMENT) {
            this.status = BookingStatus.CONFIRMED;
        }
    }

    public BigDecimal getTotalPaidAmount() {
        return payments.stream()
                .filter(payment -> payment.getStatus() == com.footArena.booking.domain.enums.PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(getTotalPaidAmount());
    }

    private String generateBookingReference() {
        return "BK" + System.currentTimeMillis() +
                String.format("%03d", (int) (Math.random() * 1000));
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public BookingType getBookingType() {
        return bookingType;
    }

    public void setBookingType(BookingType bookingType) {
        this.bookingType = bookingType;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Integer getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public void setNumberOfPlayers(Integer numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public LocalDateTime getConfirmationDeadline() {
        return confirmationDeadline;
    }

    public void setConfirmationDeadline(LocalDateTime confirmationDeadline) {
        this.confirmationDeadline = confirmationDeadline;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<BookingPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<BookingPlayer> players) {
        this.players = players;
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