package com.footArena.booking.domain.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant la participation d'un joueur dans un match
 * Gère le statut de confirmation et l'appartenance aux équipes
 */
@Entity
@Table(name = "match_players")
public class MatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "team_name")
    private String teamName; // "TEAM_A" ou "TEAM_B"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamSide teamSide; // HOME ou AWAY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerStatus status;

    @Column(name = "is_captain")
    private boolean isCaptain;

    @Column(name = "position_preference")
    private String positionPreference; // GK, DEF, MID, ATT

    @Column(name = "skill_level")
    private Integer skillLevel; // 1-10

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    // Constructeurs
    public MatchPlayer() {
    }

    public MatchPlayer(Match match, User user, String teamName) {
        this.match = match;
        this.user = user;
        this.teamName = teamName;
        this.joinedAt = LocalDateTime.now();
        this.status = PlayerStatus.PENDING;
        this.teamSide = "TEAM_A".equals(teamName) ? TeamSide.HOME : TeamSide.AWAY;
    }

    // Enums internes
    public enum PlayerStatus {
        PENDING,    // En attente de confirmation
        CONFIRMED,  // Confirmé
        DECLINED,   // Refusé
        CANCELLED,  // Annulé
        SUBSTITUTE  // Remplaçant
    }

    public enum TeamSide {
        HOME,
        AWAY
    }

    public boolean isConfirmed() {
        return status == PlayerStatus.CONFIRMED;
    }

    public boolean canPlay() {
        return status == PlayerStatus.CONFIRMED || status == PlayerStatus.SUBSTITUTE;
    }

    public void confirm() {
        this.status = PlayerStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void decline() {
        this.status = PlayerStatus.DECLINED;
    }

    public void cancel(String reason) {
        this.status = PlayerStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PlayerStatus.PENDING;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTeam() {
        return teamName;
    }

    public void setTeam(String teamName) {
        this.teamName = teamName;
    }

    public TeamSide getTeamSide() {
        return teamSide;
    }

    public void setTeamSide(TeamSide teamSide) {
        this.teamSide = teamSide;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public boolean isCaptain() {
        return isCaptain;
    }

    public void setCaptain(boolean isCaptain) {
        this.isCaptain = isCaptain;
    }

    public String getPositionPreference() {
        return positionPreference;
    }

    public void setPositionPreference(String positionPreference) {
        this.positionPreference = positionPreference;
    }

    public Integer getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(Integer skillLevel) {
        this.skillLevel = skillLevel;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
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

}