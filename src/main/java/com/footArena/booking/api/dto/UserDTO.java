package com.footArena.booking.api.dto;

import com.footArena.booking.domain.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDTO {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean enabled;
    private Role role;
    private String phoneNumber;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime emailVerifiedAt;

    public UserDTO() {
    }


    public UserDTO(UUID id, String firstName, String lastName, String email,
                   Role role, Boolean enabled,
                   String phoneNumber, String profilePictureUrl,
                   LocalDateTime createdAt, LocalDateTime lastLoginAt,
                   LocalDateTime emailVerifiedAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = profilePictureUrl;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

}