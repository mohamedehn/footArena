package com.footArena.booking.domain.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "match_players")
public class MatchPlayer {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    private Match match;

    @ManyToOne
    private User user;

    private String team;

    public MatchPlayer() {
    }

    public MatchPlayer(Match match, User user, String team) {
        this.match = match;
        this.user = user;
        this.team = team;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

}