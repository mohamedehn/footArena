package com.footArena.booking.domain.entities;

import com.footArena.booking.domain.enums.MatchStatus;
import com.footArena.booking.domain.enums.MatchType;
import com.footArena.booking.domain.enums.SkillLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private Field field;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator; // Utilisateur qui a créé le match

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private MatchType matchType = MatchType.FIVE_VS_FIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level")
    private SkillLevel skillLevel = SkillLevel.INTERMEDIATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MatchStatus status = MatchStatus.FORMING;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Column(name = "max_players_per_team", nullable = false)
    private Integer maxPlayersPerTeam;

    @Column(name = "current_players_team_a", nullable = false)
    private Integer currentPlayersTeamA = 0;

    @Column(name = "current_players_team_b", nullable = false)
    private Integer currentPlayersTeamB = 0;

    @Column(name = "min_players_to_start")
    private Integer minPlayersToStart;

    @Column(name = "auto_start", nullable = false)
    private Boolean autoStart = false; // Démarrage automatique quand les équipes sont complètes

    @Column(name = "allow_substitutes", nullable = false)
    private Boolean allowSubstitutes = true;

    @Column(name = "entry_fee", precision = 10, scale = 2)
    private java.math.BigDecimal entryFee;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "score_team_a")
    private Integer scoreTeamA;

    @Column(name = "score_team_b")
    private Integer scoreTeamB;

    @Column(name = "winner_team")
    private String winnerTeam; // "TEAM_A", "TEAM_B", "DRAW"

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchPlayer> players = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Match() {
    }

    public Match(Field field, Slot slot, User creator, String title, MatchType matchType) {
        this.field = field;
        this.slot = slot;
        this.creator = creator;
        this.title = title;
        this.matchType = matchType;
        this.status = MatchStatus.FORMING;
        this.isPublic = true;

        // Définir le nombre de joueurs selon le type
        switch (matchType) {
            case FIVE_VS_FIVE:
                this.maxPlayersPerTeam = 5;
                this.minPlayersToStart = 8; // 4 par équipe minimum
                break;
            case SEVEN_VS_SEVEN:
                this.maxPlayersPerTeam = 7;
                this.minPlayersToStart = 10;
                break;
            case ELEVEN_VS_ELEVEN:
                this.maxPlayersPerTeam = 11;
                this.minPlayersToStart = 16;
                break;
            default:
                this.maxPlayersPerTeam = 5;
                this.minPlayersToStart = 6;
        }

        this.registrationDeadline = slot.getStartTime().minusMinutes(30);
    }

    // Méthodes métier
    public boolean isFull() {
        return currentPlayersTeamA >= maxPlayersPerTeam &&
                currentPlayersTeamB >= maxPlayersPerTeam;
    }

    public boolean canStart() {
        return getTotalPlayers() >= minPlayersToStart;
    }

    public boolean hasSpace() {
        return !isFull();
    }

    public boolean hasSpaceInTeamA() {
        return currentPlayersTeamA < maxPlayersPerTeam;
    }

    public boolean hasSpaceInTeamB() {
        return currentPlayersTeamB < maxPlayersPerTeam;
    }

    public int getTotalPlayers() {
        return currentPlayersTeamA + currentPlayersTeamB;
    }

    public int getAvailableSpots() {
        return (maxPlayersPerTeam * 2) - getTotalPlayers();
    }

    public String getBalancedTeamForNewPlayer() {
        if (currentPlayersTeamA <= currentPlayersTeamB && hasSpaceInTeamA()) {
            return "TEAM_A";
        } else if (hasSpaceInTeamB()) {
            return "TEAM_B";
        }
        return null; // Match complet
    }

    public void addPlayerToTeam(String team) {
        if ("TEAM_A".equals(team)) {
            currentPlayersTeamA++;
        } else if ("TEAM_B".equals(team)) {
            currentPlayersTeamB++;
        }

        // Vérifier si le match peut démarrer automatiquement
        if (autoStart && isFull()) {
            this.status = MatchStatus.CONFIRMED;
        }
    }

    public void removePlayerFromTeam(String team) {
        if ("TEAM_A".equals(team) && currentPlayersTeamA > 0) {
            currentPlayersTeamA--;
        } else if ("TEAM_B".equals(team) && currentPlayersTeamB > 0) {
            currentPlayersTeamB--;
        }
    }

    public void startMatch() {
        this.status = MatchStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void completeMatch(Integer scoreA, Integer scoreB) {
        this.status = MatchStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.scoreTeamA = scoreA;
        this.scoreTeamB = scoreB;

        if (scoreA > scoreB) {
            this.winnerTeam = "TEAM_A";
        } else if (scoreB > scoreA) {
            this.winnerTeam = "TEAM_B";
        } else {
            this.winnerTeam = "DRAW";
        }
    }

    public boolean isRegistrationOpen() {
        return registrationDeadline == null || LocalDateTime.now().isBefore(registrationDeadline);
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

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Integer getMaxPlayersPerTeam() {
        return maxPlayersPerTeam;
    }

    public void setMaxPlayersPerTeam(Integer maxPlayersPerTeam) {
        this.maxPlayersPerTeam = maxPlayersPerTeam;
    }

    public Integer getCurrentPlayersTeamA() {
        return currentPlayersTeamA;
    }

    public void setCurrentPlayersTeamA(Integer currentPlayersTeamA) {
        this.currentPlayersTeamA = currentPlayersTeamA;
    }

    public Integer getCurrentPlayersTeamB() {
        return currentPlayersTeamB;
    }

    public void setCurrentPlayersTeamB(Integer currentPlayersTeamB) {
        this.currentPlayersTeamB = currentPlayersTeamB;
    }

    public Integer getMinPlayersToStart() {
        return minPlayersToStart;
    }

    public void setMinPlayersToStart(Integer minPlayersToStart) {
        this.minPlayersToStart = minPlayersToStart;
    }

    public Boolean getAutoStart() {
        return autoStart;
    }

    public void setAutoStart(Boolean autoStart) {
        this.autoStart = autoStart;
    }

    public Boolean getAllowSubstitutes() {
        return allowSubstitutes;
    }

    public void setAllowSubstitutes(Boolean allowSubstitutes) {
        this.allowSubstitutes = allowSubstitutes;
    }

    public java.math.BigDecimal getEntryFee() {
        return entryFee;
    }

    public void setEntryFee(java.math.BigDecimal entryFee) {
        this.entryFee = entryFee;
    }

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getScoreTeamA() {
        return scoreTeamA;
    }

    public void setScoreTeamA(Integer scoreTeamA) {
        this.scoreTeamA = scoreTeamA;
    }

    public Integer getScoreTeamB() {
        return scoreTeamB;
    }

    public void setScoreTeamB(Integer scoreTeamB) {
        this.scoreTeamB = scoreTeamB;
    }

    public String getWinnerTeam() {
        return winnerTeam;
    }

    public void setWinnerTeam(String winnerTeam) {
        this.winnerTeam = winnerTeam;
    }

    public List<MatchPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<MatchPlayer> players) {
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