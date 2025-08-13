package com.footArena.booking.api.dto.request;

import com.footArena.booking.domain.enums.BookingType;
import jakarta.validation.constraints.*;

import java.util.UUID;

public class CreateBookingRequest {

    @NotNull(message = "Slot ID is required")
    private UUID slotId;

    @NotNull(message = "Booking type is required")
    private BookingType bookingType;

    @NotNull(message = "Number of players is required")
    @Min(value = 1, message = "At least 1 player is required")
    @Max(value = 22, message = "Cannot exceed 22 players")
    private Integer numberOfPlayers;

    @Size(max = 100, message = "Team name cannot exceed 100 characters")
    private String teamName;

    @Size(max = 500, message = "Special requests cannot exceed 500 characters")
    private String specialRequests;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String contactPhone;

    public CreateBookingRequest() {
    }

    public CreateBookingRequest(UUID slotId, BookingType bookingType, Integer numberOfPlayers) {
        this.slotId = slotId;
        this.bookingType = bookingType;
        this.numberOfPlayers = numberOfPlayers;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public void setSlotId(UUID slotId) {
        this.slotId = slotId;
    }

    public BookingType getBookingType() {
        return bookingType;
    }

    public void setBookingType(BookingType bookingType) {
        this.bookingType = bookingType;
    }

    public Integer getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public void setNumberOfPlayers(Integer numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}