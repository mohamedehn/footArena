package com.footArena.booking.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class EstablishmentResponse {

    private UUID id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FieldResponse> fields;
    private Integer totalFields;
    private Integer availableFields;

    public EstablishmentResponse() {
    }

    public EstablishmentResponse(UUID id, String name, String address, String phone,
                                 String email, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters et Setters
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public List<FieldResponse> getFields() {
        return fields;
    }

    public void setFields(List<FieldResponse> fields) {
        this.fields = fields;
    }

    public Integer getTotalFields() {
        return totalFields;
    }

    public void setTotalFields(Integer totalFields) {
        this.totalFields = totalFields;
    }

    public Integer getAvailableFields() {
        return availableFields;
    }

    public void setAvailableFields(Integer availableFields) {
        this.availableFields = availableFields;
    }
}