package com.footArena.booking.domain.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_players")
public class BookingPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "player_name", nullable = false)
    private String playerName; // Nom affiché pour le match

    @Column(name = "position")
    private String position; // Poste préféré (gardien, défenseur, etc.)

    @Column(name = "team_side")
    private String teamSide; // Équipe A ou B pour les matchs

    @Column(name = "is_captain", nullable = false)
    private Boolean isCaptain = false;

    @Column(name = "status", nullable = false)
    private String status = "CONFIRMED"; // CONFIRMED, PENDING, CANCELLED

    @Column(name = "joined_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime joinedAt;

    public BookingPlayer() {
    }

    public BookingPlayer(Booking booking, User user, String playerName) {
        this.booking = booking;
        this.user = user;
        this.playerName = playerName;
        this.isCaptain = false;
        this.status = "CONFIRMED";
    }

    public boolean isConfirmed() {
        return "CONFIRMED".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public void confirm() {
        this.status = "CONFIRMED";
    }

    public void cancel() {
        this.status = "CANCELLED";
    }

    public void makeCaptain() {
        this.isCaptain = true;
    }

    public void removeCaptaincy() {
        this.isCaptain = false;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getTeamSide() {
        return teamSide;
    }

    public void setTeamSide(String teamSide) {
        this.teamSide = teamSide;
    }

    public Boolean getIsCaptain() {
        return isCaptain;
    }

    public void setIsCaptain(Boolean isCaptain) {
        this.isCaptain = isCaptain;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}