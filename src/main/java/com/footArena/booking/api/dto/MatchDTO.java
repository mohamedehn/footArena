package com.footArena.booking.api.dto;

import com.footArena.booking.domain.model.enums.MatchStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class MatchDTO {

    private UUID id;
    private UUID fieldId;
    private String type;
    private String level;
    private boolean isPublic;
    private boolean isFull;
    private MatchStatus matchStatus;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public MatchDTO() {
    }

    public MatchDTO(UUID id, UUID fieldId, String type, String level, boolean isPublic, boolean isFull, MatchStatus matchStatus, String description, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.id = id;
        this.fieldId = fieldId;
        this.type = type;
        this.level = level;
        this.isPublic = isPublic;
        this.isFull = isFull;
        this.matchStatus = matchStatus;
        this.description = description;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFieldId() {
        return fieldId;
    }

    public void setFieldId(UUID fieldId) {
        this.fieldId = fieldId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean isFull) {
        this.isFull = isFull;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(MatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

}