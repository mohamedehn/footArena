package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.response.FieldResponse;
import com.footArena.booking.domain.entities.Field;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FieldMapper {

    /**
     * Convertit une entité Field en FieldResponse
     */
    public FieldResponse toResponse(Field field) {
        if (field == null) {
            return null;
        }

        FieldResponse response = new FieldResponse();
        response.setId(field.getId());
        response.setName(field.getName());
        response.setLocation(field.getLocation());
        response.setSurfaceType(field.getSurfaceType());
        response.setCapacity(field.getCapacity());
        response.setAvailable(field.isAvailable());

        // Informations de l'établissement
        if (field.getEstablishment() != null) {
            response.setEstablishmentId(field.getEstablishment().getId());
            response.setEstablishmentName(field.getEstablishment().getName());
        }

        return response;
    }

    /**
     * Convertit une liste d'entités Field en liste de FieldResponse
     */
    public List<FieldResponse> toResponseList(List<Field> fields) {
        if (fields == null) {
            return List.of();
        }

        return fields.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}