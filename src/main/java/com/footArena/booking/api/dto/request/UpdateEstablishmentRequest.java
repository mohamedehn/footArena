package com.footArena.booking.api.dto.request;

import jakarta.validation.constraints.*;

public class UpdateEstablishmentRequest {

    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String name;

    @Size(min = 5, max = 255, message = "L'adresse doit contenir entre 5 et 255 caractères")
    private String address;

    @Pattern(regexp = "^[0-9]{10}$", message = "Le téléphone doit contenir exactement 10 chiffres")
    private String phone;

    @Email(message = "Format d'email invalide")
    private String email;

    public UpdateEstablishmentRequest() {
    }

    public UpdateEstablishmentRequest(String name, String address, String phone, String email) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
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
}