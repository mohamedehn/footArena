package com.footArena.booking.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CompleteMatchRequest {

    @NotNull(message = "Score Team A is required")
    @Min(value = 0, message = "Score must be positive")
    private Integer scoreTeamA;

    @NotNull(message = "Score Team B is required")
    @Min(value = 0, message = "Score must be positive")
    private Integer scoreTeamB;

    private String matchReport; // Rapport optionnel du match

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

    public String getMatchReport() {
        return matchReport;
    }

    public void setMatchReport(String matchReport) {
        this.matchReport = matchReport;
    }
}