package com.footArena.booking.api.dto;

import java.util.UUID;

public class FieldDTO {

    private UUID id;
    private String name;
    private String location;
    private String surfaceType;
    private int capacity;
    private boolean available;
    private UUID establishmentId;

    public FieldDTO() {
    }

    public FieldDTO(UUID id, String name, String location, String surfaceType, int capacity, boolean available, UUID establishmentId) {
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public UUID getEstablishmentId() {
        return establishmentId;
    }

    public void setEstablishmentId(UUID establishmentId) {
        this.establishmentId = establishmentId;
    }

}