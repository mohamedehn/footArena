package com.footArena.booking.api.dto.response;

import com.footArena.booking.domain.enums.MatchStatus;
import com.footArena.booking.domain.enums.MatchType;
import com.footArena.booking.domain.enums.SkillLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MatchResponse {

    private UUID id;
    private FieldResponse field;
    private SlotResponse slot;
    private UserResponse creator;
    private String title;
    private String description;
    private MatchType matchType;
    private SkillLevel skillLevel;
    private MatchStatus status;
    private Boolean isPublic;
    private Integer maxPlayersPerTeam;
    private Integer currentPlayersTeamA;
    private Integer currentPlayersTeamB;
    private Integer minPlayersToStart;
    private Boolean autoStart;
    private Boolean allowSubstitutes;
    private BigDecimal entryFee;
    private LocalDateTime registrationDeadline;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer scoreTeamA;
    private Integer scoreTeamB;
    private String winnerTeam;
    private List<MatchPlayerResponse> players;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields
    private Boolean isFull;
    private Boolean canStart;
    private Integer totalPlayers;
    private Integer availableSpots;
    private Boolean isRegistrationOpen;
    private String matchDuration;
    private String location;

    // Getters et Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FieldResponse getField() {
        return field;
    }

    public void setField(FieldResponse field) {
        this.field = field;
    }

    public SlotResponse getSlot() {
        return slot;
    }

    public void setSlot(SlotResponse slot) {
        this.slot = slot;
    }

    public UserResponse getCreator() {
        return creator;
    }

    public void setCreator(UserResponse creator) {
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

    public BigDecimal getEntryFee() {
        return entryFee;
    }

    public void setEntryFee(BigDecimal entryFee) {
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

    public List<MatchPlayerResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<MatchPlayerResponse> players) {
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

    public Boolean getIsFull() {
        return isFull;
    }

    public void setIsFull(Boolean isFull) {
        this.isFull = isFull;
    }

    public Boolean getCanStart() {
        return canStart;
    }

    public void setCanStart(Boolean canStart) {
        this.canStart = canStart;
    }

    public Integer getTotalPlayers() {
        return totalPlayers;
    }

    public void setTotalPlayers(Integer totalPlayers) {
        this.totalPlayers = totalPlayers;
    }

    public Integer getAvailableSpots() {
        return availableSpots;
    }

    public void setAvailableSpots(Integer availableSpots) {
        this.availableSpots = availableSpots;
    }

    public Boolean getIsRegistrationOpen() {
        return isRegistrationOpen;
    }

    public void setIsRegistrationOpen(Boolean isRegistrationOpen) {
        this.isRegistrationOpen = isRegistrationOpen;
    }

    public String getMatchDuration() {
        return matchDuration;
    }

    public void setMatchDuration(String matchDuration) {
        this.matchDuration = matchDuration;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}