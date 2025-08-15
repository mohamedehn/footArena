package com.footArena.booking.api.dto.request;

import jakarta.validation.constraints.Pattern;

public class JoinMatchRequest {

    @Pattern(regexp = "^(TEAM_A|TEAM_B)$", message = "Team must be TEAM_A or TEAM_B")
    private String preferredTeam; // Optionnel, sinon assignation automatique

    private String position; // Gardien, Défenseur, Milieu, Attaquant

    private String playerName; // Nom affiché pour le match

    public String getPreferredTeam() {
        return preferredTeam;
    }

    public void setPreferredTeam(String preferredTeam) {
        this.preferredTeam = preferredTeam;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}