package com.footArena.booking.api.dto.request;

import com.footArena.booking.domain.enums.MatchStatus;
import com.footArena.booking.domain.enums.SkillLevel;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateMatchRequest {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private SkillLevel skillLevel;

    private Boolean isPublic;

    private Boolean autoStart;

    private Boolean allowSubstitutes;

    private BigDecimal entryFee;

    private MatchStatus status;

    // Getters et Setters
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

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
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

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }
}