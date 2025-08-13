package com.footArena.booking.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class BookingPlayerResponse {

    private UUID id;
    private UserResponse user;
    private String playerName;
    private String position;
    private String teamSide;
    private Boolean isCaptain;
    private String status;
    private LocalDateTime joinedAt;

    public BookingPlayerResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
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