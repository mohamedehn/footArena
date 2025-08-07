package com.footArena.booking.api.dto.response;

import java.util.UUID;

public class FieldResponse {

    private UUID id;
    private String name;
    private String location;
    private String surfaceType;
    private Integer capacity;
    private Boolean available;
    private UUID establishmentId;
    private String establishmentName;

    public FieldResponse() {
    }

    public FieldResponse(UUID id, String name, String location, String surfaceType,
                         Integer capacity, Boolean available, UUID establishmentId) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.surfaceType = surfaceType;
        this.capacity = capacity;
        this.available = available;
        this.establishmentId = establishmentId;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSurfaceType() {
        return surfaceType;
    }

    public void setSurfaceType(String surfaceType) {
        this.surfaceType = surfaceType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public UUID getEstablishmentId() {
        return establishmentId;
    }

    public void setEstablishmentId(UUID establishmentId) {
        this.establishmentId = establishmentId;
    }

    public String getEstablishmentName() {
        return establishmentName;
    }

    public void setEstablishmentName(String establishmentName) {
        this.establishmentName = establishmentName;
    }
}