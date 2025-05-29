package com.footArena.booking.api.mapper;

import com.footArena.booking.api.dto.FieldDTO;
import com.footArena.booking.domain.model.entity.Field;

public class FieldMapper {
    public static FieldDTO MappedFieldToDto(Field field) {
        FieldDTO fieldDTO = new FieldDTO();
        fieldDTO.setId(field.getId());
        fieldDTO.setName(field.getName());
        fieldDTO.setLocation(field.getLocation());
        fieldDTO.setSurfaceType(field.getSurfaceType());
        fieldDTO.setCapacity(field.getCapacity());
        fieldDTO.setAvailable(field.isAvailable());
        fieldDTO.setEstablishmentId(field.getEstablishment().getId());
        return fieldDTO;
    }

    public static Field MappedFieldToEntity(FieldDTO fieldDTO) {
        Field field = new Field();
        field.setId(fieldDTO.getId());
        field.setName(fieldDTO.getName());
        field.setLocation(fieldDTO.getLocation());
        field.setSurfaceType(fieldDTO.getSurfaceType());
        field.setCapacity(fieldDTO.getCapacity());
        field.setAvailable(fieldDTO.isAvailable());
        // Assuming Establishment is mapped elsewhere -- TODO with service or repository
        return field;
    }
}