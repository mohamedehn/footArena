package com.footArena.booking.domain.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captain_id", nullable = false)
    private User captain;

    @Column(name = "description")
    private String description;

    @Column(name = "max_players", nullable = false)
    private Integer maxPlayers;

    @Column(name = "current_players", nullable = false)
    private Integer currentPlayers = 0;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Column(name = "skill_level")
    @Enumerated(EnumType.STRING)
    private com.footArena.booking.domain.enums.SkillLevel skillLevel;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> members = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Team() {
    }

    public Team(String name, User captain, Integer maxPlayers) {
        this.name = name;
        this.captain = captain;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = 1; // Le capitaine compte
        this.isPublic = true;
    }

    public boolean isFull() {
        return currentPlayers >= maxPlayers;
    }

    public boolean hasSpace() {
        return currentPlayers < maxPlayers;
    }

    public int getAvailableSpots() {
        return maxPlayers - currentPlayers;
    }

    public void addMember() {
        this.currentPlayers++;
    }

    public void removeMember() {
        if (this.currentPlayers > 0) {
            this.currentPlayers--;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getCaptain() {
        return captain;
    }

    public void setCaptain(User captain) {
        this.captain = captain;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Integer getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(Integer currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public com.footArena.booking.domain.enums.SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(com.footArena.booking.domain.enums.SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
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