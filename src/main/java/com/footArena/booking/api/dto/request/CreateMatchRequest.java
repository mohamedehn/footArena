package com.footArena.booking.api.dto.request;

import com.footArena.booking.domain.enums.MatchType;
import com.footArena.booking.domain.enums.SkillLevel;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CreateMatchRequest {

    @NotNull(message = "Field ID is required")
    private UUID fieldId;

    @NotNull(message = "Slot ID is required")
    private UUID slotId;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Match type is required")
    private MatchType matchType = MatchType.FIVE_VS_FIVE;

    private SkillLevel skillLevel = SkillLevel.INTERMEDIATE;

    @NotNull(message = "Public status is required")
    private Boolean isPublic = true;

    private Boolean autoStart = false;

    private Boolean allowSubstitutes = true;

    @DecimalMin(value = "0.0", message = "Entry fee must be positive")
    private BigDecimal entryFee;

    private LocalDateTime registrationDeadline;

    public UUID getFieldId() {
        return fieldId;
    }

    public void setFieldId(UUID fieldId) {
        this.fieldId = fieldId;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public void setSlotId(UUID slotId) {
        this.slotId = slotId;
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

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }
}