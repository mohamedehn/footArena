package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.request.CreateEstablishmentRequest;
import com.footArena.booking.api.dto.request.UpdateEstablishmentRequest;
import com.footArena.booking.api.dto.response.EstablishmentResponse;
import com.footArena.booking.domain.entities.Establishment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EstablishmentMapper {

    private final FieldMapper fieldMapper;

    public EstablishmentMapper(FieldMapper fieldMapper) {
        this.fieldMapper = fieldMapper;
    }

    /**
     * Convertit une CreateEstablishmentRequest en entité Establishment
     */
    public Establishment toEntity(CreateEstablishmentRequest request) {
        if (request == null) {
            return null;
        }

        return new Establishment(
                request.getName(),
                request.getAddress(),
                request.getPhone(),
                request.getEmail()
        );
    }

    /**
     * Met à jour une entité Establishment avec les données d'UpdateEstablishmentRequest
     */
    public void updateEntity(Establishment establishment, UpdateEstablishmentRequest request) {
        if (establishment == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            establishment.setName(request.getName());
        }
        if (request.getAddress() != null) {
            establishment.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            establishment.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            establishment.setEmail(request.getEmail());
        }
    }

    /**
     * Convertit une entité Establishment en EstablishmentResponse
     */
    public EstablishmentResponse toResponse(Establishment establishment) {
        if (establishment == null) {
            return null;
        }

        EstablishmentResponse response = new EstablishmentResponse();
        response.setId(establishment.getId());
        response.setName(establishment.getName());
        response.setAddress(establishment.getAddress());
        response.setPhone(establishment.getPhone());
        response.setEmail(establishment.getEmail());

        // Conversion des dates
        if (establishment.getCreatedAt() != null) {
            response.setCreatedAt(LocalDateTime.ofInstant(
                    establishment.getCreatedAt().toInstant(),
                    ZoneId.systemDefault()
            ));
        }

        if (establishment.getUpdatedAt() != null) {
            response.setUpdatedAt(LocalDateTime.ofInstant(
                    establishment.getUpdatedAt().toInstant(),
                    ZoneId.systemDefault()
            ));
        }

        // Conversion des terrains
        if (establishment.getFields() != null) {
            response.setFields(establishment.getFields().stream()
                    .map(fieldMapper::toResponse)
                    .collect(Collectors.toList()));

            response.setTotalFields(establishment.getFields().size());
            response.setAvailableFields((int) establishment.getFields().stream()
                    .filter(field -> field.isAvailable())
                    .count());
        } else {
            response.setTotalFields(0);
            response.setAvailableFields(0);
        }

        return response;
    }

    /**
     * Convertit une liste d'entités en liste de réponses
     */
    public List<EstablishmentResponse> toResponseList(List<Establishment> establishments) {
        if (establishments == null) {
            return List.of();
        }

        return establishments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}