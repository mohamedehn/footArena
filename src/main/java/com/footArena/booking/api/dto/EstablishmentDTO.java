package com.footArena.booking.api.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EstablishmentDTO {

    private UUID id;
    private String name;
    private String address;
    private String email;
    private String phone;
    private Date createdAt;
    private Date updatedAt;
    private List<FieldDTO> fields = new ArrayList<>();

    public EstablishmentDTO() {
    }

    public EstablishmentDTO(UUID id, String name, String address, String email, String phone, Date createdAt, Date updatedAt, List<FieldDTO> fields) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fields = fields;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<FieldDTO> getFields() {
        return fields;
    }

    public void setFields(List<FieldDTO> fields) {
        this.fields = fields;
    }

    public void addField(FieldDTO field) {
        if (this.fields == null) {
            this.fields = new ArrayList<>();
        }
        this.fields.add(field);
    }

    public void removeField(FieldDTO field) {
        if (this.fields != null) {
            this.fields.remove(field);
        }
    }

}