package com.footArena.booking.domain.entities;

import com.footArena.booking.domain.enums.Role;
import com.footArena.booking.domain.enums.SkillLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "profile_picture_url")
    @Lob
    private String profilePictureUrl; // URL ou base64

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expires_at")
    private LocalDateTime passwordResetTokenExpiresAt;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // ===== Champs Matchmaking =====

    @Column(name = "skill_level")
    private SkillLevel skillLevel;

    @Column(name = "preferred_position")
    private String preferredPosition;

    @Column(name = "preferred_foot")
    private String preferredFoot;

    @Column(name = "location_latitude")
    private Double locationLatitude;

    @Column(name = "location_longitude")
    private Double locationLongitude;

    @Column(name = "max_distance_km")
    private Integer maxDistanceKm;

    @Column(name = "preferred_days", columnDefinition = "TEXT")
    private String preferredDays;

    @Column(name = "preferred_time_slots", columnDefinition = "TEXT")
    private String preferredTimeSlots;

    @Column(name = "matches_played")
    private Integer matchesPlayed = 0;

    @Column(name = "matches_won")
    private Integer matchesWon = 0;

    @Column(name = "matches_drawn")
    private Integer matchesDrawn = 0;

    @Column(name = "matches_lost")
    private Integer matchesLost = 0;

    @Column(name = "reliability_score")
    private Double reliabilityScore = 100.0;

    @Column(name = "fair_play_score")
    private Double fairPlayScore = 5.0;

    @Column(name = "last_match_date")
    private LocalDateTime lastMatchDate;

    @Column(name = "accept_auto_match")
    private Boolean acceptAutoMatch = true;

    @Column(name = "min_skill_level")
    private Integer minSkillLevel;

    @Column(name = "max_skill_level")
    private Integer maxSkillLevel;

    // Relations
    @OneToMany(mappedBy = "captain", fetch = FetchType.LAZY)
    private Set<Team> captainedTeams = new HashSet<>();

    // Relation via TeamMember
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<TeamMember> teamMemberships = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Booking> bookings = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<MatchPlayer> matchParticipations = new HashSet<>();

    public User() {
    }

    public User(String firstName, String lastName, String email, String password, boolean isActive, Role role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.isActive = isActive;
        this.role = role;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public void lockAccount(int minutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(minutes);
    }

    public void unlockAccount() {
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void verifyEmail() {
        this.emailVerifiedAt = LocalDateTime.now();
        this.emailVerificationToken = null;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Personnaliser si nécessaire
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isAccountLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Personnaliser si expiration mot de passe
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public LocalDateTime getPasswordResetTokenExpiresAt() {
        return passwordResetTokenExpiresAt;
    }

    public void setPasswordResetTokenExpiresAt(LocalDateTime passwordResetTokenExpiresAt) {
        this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public String getPreferredPosition() {
        return preferredPosition;
    }

    public void setPreferredPosition(String preferredPosition) {
        this.preferredPosition = preferredPosition;
    }

    public String getPreferredFoot() {
        return preferredFoot;
    }

    public void setPreferredFoot(String preferredFoot) {
        this.preferredFoot = preferredFoot;
    }

    public Double getLocationLatitude() {
        return locationLatitude;
    }

    public void setLocationLatitude(Double locationLatitude) {
        this.locationLatitude = locationLatitude;
    }

    public Double getLocationLongitude() {
        return locationLongitude;
    }

    public void setLocationLongitude(Double locationLongitude) {
        this.locationLongitude = locationLongitude;
    }

    public Integer getMaxDistanceKm() {
        return maxDistanceKm;
    }

    public void setMaxDistanceKm(Integer maxDistanceKm) {
        this.maxDistanceKm = maxDistanceKm;
    }

    public String getPreferredDays() {
        return preferredDays;
    }

    public void setPreferredDays(String preferredDays) {
        this.preferredDays = preferredDays;
    }

    public String getPreferredTimeSlots() {
        return preferredTimeSlots;
    }

    public void setPreferredTimeSlots(String preferredTimeSlots) {
        this.preferredTimeSlots = preferredTimeSlots;
    }

    public Integer getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public Integer getMatchesWon() {
        return matchesWon;
    }

    public void setMatchesWon(Integer matchesWon) {
        this.matchesWon = matchesWon;
    }

    public Integer getMatchesDrawn() {
        return matchesDrawn;
    }

    public void setMatchesDrawn(Integer matchesDrawn) {
        this.matchesDrawn = matchesDrawn;
    }

    public Integer getMatchesLost() {
        return matchesLost;
    }

    public void setMatchesLost(Integer matchesLost) {
        this.matchesLost = matchesLost;
    }

    public Double getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(Double reliabilityScore) {
        this.reliabilityScore = reliabilityScore;
    }

    public Double getFairPlayScore() {
        return fairPlayScore;
    }

    public void setFairPlayScore(Double fairPlayScore) {
        this.fairPlayScore = fairPlayScore;
    }

    public LocalDateTime getLastMatchDate() {
        return lastMatchDate;
    }

    public void setLastMatchDate(LocalDateTime lastMatchDate) {
        this.lastMatchDate = lastMatchDate;
    }

    public Boolean getAcceptAutoMatch() {
        return acceptAutoMatch;
    }

    public void setAcceptAutoMatch(Boolean acceptAutoMatch) {
        this.acceptAutoMatch = acceptAutoMatch;
    }

    public Integer getMinSkillLevel() {
        return minSkillLevel;
    }

    public void setMinSkillLevel(Integer minSkillLevel) {
        this.minSkillLevel = minSkillLevel;
    }

    public Integer getMaxSkillLevel() {
        return maxSkillLevel;
    }

    public void setMaxSkillLevel(Integer maxSkillLevel) {
        this.maxSkillLevel = maxSkillLevel;
    }

    public Set<Team> getCaptainedTeams() {
        return captainedTeams;
    }

    public void setCaptainedTeams(Set<Team> captainedTeams) {
        this.captainedTeams = captainedTeams;
    }

    public Set<TeamMember> getTeamMemberships() {
        return teamMemberships;
    }

    public void setTeamMemberships(Set<TeamMember> teamMemberships) {
        this.teamMemberships = teamMemberships;
    }

    public Set<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(Set<Booking> bookings) {
        this.bookings = bookings;
    }

    public Set<MatchPlayer> getMatchParticipations() {
        return matchParticipations;
    }

    public void setMatchParticipations(Set<MatchPlayer> matchParticipations) {
        this.matchParticipations = matchParticipations;
    }

}