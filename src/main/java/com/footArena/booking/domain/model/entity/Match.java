package com.footArena.booking.domain.model.entity;

import com.footArena.booking.domain.model.enums.MatchStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    private Field field;

    private String type;

    private String level;

    private boolean isPublic;

    private boolean isFull;

    @Enumerated(EnumType.STRING)
    private MatchStatus matchStatus;

    private String description;

    private java.time.LocalDateTime startDateTime;

    private java.time.LocalDateTime endDateTime;

    public Match() {
    }

    public Match(Field field, String type, String level, boolean isPublic, boolean isFull, MatchStatus matchStatus, String description, java.time.LocalDateTime startDateTime, java.time.LocalDateTime endDateTime) {
        this.field = field;
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

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
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

    public java.time.LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(java.time.LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public java.time.LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(java.time.LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }
}