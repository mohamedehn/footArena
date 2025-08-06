package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.EstablishmentDTO;
import com.footArena.booking.api.dto.FieldDTO;
import com.footArena.booking.domain.entities.Establishment;
import com.footArena.booking.domain.entities.Field;

import java.util.List;

public class EstablishmentMapper {
    public static EstablishmentDTO MappedEstablishmentToDto(Establishment establishment) {
        EstablishmentDTO establishmentDTO = new EstablishmentDTO();
        establishmentDTO.setId(establishment.getId());
        establishmentDTO.setName(establishment.getName());
        establishmentDTO.setAddress(establishment.getAddress());
        establishmentDTO.setPhone(establishment.getPhone());
        establishmentDTO.setEmail(establishment.getEmail());
        establishmentDTO.setCreatedAt(establishment.getCreatedAt());
        establishmentDTO.setUpdatedAt(establishment.getUpdatedAt());
        List<FieldDTO> fieldDTOs = establishment.getFields().stream()
                .map(FieldMapper::MappedFieldToDto)
                .toList();
        return establishmentDTO;
    }

    public static Establishment MappedEstablishmentToEntity(EstablishmentDTO establishmentDTO) {
        Establishment establishment = new Establishment();
        establishment.setId(establishmentDTO.getId());
        establishment.setName(establishmentDTO.getName());
        establishment.setAddress(establishmentDTO.getAddress());
        establishment.setPhone(establishmentDTO.getPhone());
        establishment.setEmail(establishmentDTO.getEmail());
        establishment.setCreatedAt(establishmentDTO.getCreatedAt());
        establishment.setUpdatedAt(establishmentDTO.getUpdatedAt());
        List<Field> fields = establishmentDTO.getFields().stream()
                .map(FieldMapper::MappedFieldToEntity)
                .toList();

        fields.forEach(field -> field.setEstablishment(establishment));
        establishment.setFields(fields);
        return establishment;
    }

}