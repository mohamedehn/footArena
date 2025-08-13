package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.response.SlotResponse;
import com.footArena.booking.domain.entities.Slot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SlotMapper {

    /**
     * Convertit une entité Slot en SlotResponse
     */
    public SlotResponse toResponse(Slot slot) {
        if (slot == null) {
            return null;
        }

        SlotResponse response = new SlotResponse();
        response.setId(slot.getId());
        response.setFieldId(slot.getField().getId());
        response.setFieldName(slot.getField().getName());
        response.setEstablishmentName(slot.getField().getEstablishment().getName());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setPrice(slot.getPrice());
        response.setStatus(slot.getStatus());
        response.setMaxCapacity(slot.getMaxCapacity());
        response.setCurrentBookings(slot.getCurrentBookings());
        response.setAvailableSpots(slot.getAvailableSpots());
        response.setDescription(slot.getDescription());
        response.setIsPremium(slot.getIsPremium());
        response.setCancellationDeadlineHours(slot.getCancellationDeadlineHours());
        response.setDurationInMinutes(slot.getDurationInMinutes());
        response.setCanBeCancelled(slot.canBeCancelled());
        response.setIsToday(slot.isToday());
        response.setCreatedAt(slot.getCreatedAt());
        response.setUpdatedAt(slot.getUpdatedAt());

        return response;
    }

    /**
     * Convertit une liste d'entités Slot en liste de SlotResponse
     */
    public List<SlotResponse> toResponseList(List<Slot> slots) {
        if (slots == null) {
            return List.of();
        }

        return slots.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une entité Slot en SlotResponse simplifié (sans relations)
     */
    public SlotResponse toSimpleResponse(Slot slot) {
        if (slot == null) {
            return null;
        }

        SlotResponse response = new SlotResponse();
        response.setId(slot.getId());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setPrice(slot.getPrice());
        response.setStatus(slot.getStatus());
        response.setMaxCapacity(slot.getMaxCapacity());
        response.setCurrentBookings(slot.getCurrentBookings());
        response.setAvailableSpots(slot.getAvailableSpots());
        response.setIsPremium(slot.getIsPremium());
        response.setDurationInMinutes(slot.getDurationInMinutes());

        return response;
    }
}