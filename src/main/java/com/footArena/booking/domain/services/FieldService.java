package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.Establishment;
import com.footArena.booking.domain.entities.Field;
import com.footArena.booking.domain.exceptions.BusinessValidationException;
import com.footArena.booking.domain.exceptions.ResourceNotFoundException;
import com.footArena.booking.domain.repositories.FieldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FieldService {

    private static final Logger logger = LoggerFactory.getLogger(FieldService.class);

    private final FieldRepository fieldRepository;
    private final EstablishmentService establishmentService;

    public FieldService(FieldRepository fieldRepository, EstablishmentService establishmentService) {
        this.fieldRepository = fieldRepository;
        this.establishmentService = establishmentService;
    }

    /**
     * Crée un nouveau terrain
     */
    public Field createField(String name, String location, String surfaceType,
                             int capacity, boolean available, UUID establishmentId) {
        logger.info("Creating new field: {} for establishment: {}", name, establishmentId);

        // Validation métier
        validateFieldData(name, location, surfaceType, capacity);
        Establishment establishment = establishmentService.getEstablishmentById(establishmentId);

        Field field = new Field(name, location, surfaceType, capacity, available, establishment);

        Field savedField = fieldRepository.save(field);
        logger.info("Field created with ID: {}", savedField.getId());

        return savedField;
    }

    /**
     * Met à jour un terrain existant
     */
    public Field updateField(UUID id, String name, String location, String surfaceType,
                             Integer capacity, Boolean available) {
        logger.info("Updating field with ID: {}", id);

        Field field = getFieldById(id);

        if (name != null) {
            validateName(name);
            field.setName(name);
        }

        if (location != null) {
            validateLocation(location);
            field.setLocation(location);
        }

        if (surfaceType != null) {
            validateSurfaceType(surfaceType);
            field.setSurfaceType(surfaceType);
        }

        if (capacity != null) {
            validateCapacity(capacity);
            field.setCapacity(capacity);
        }

        if (available != null) {
            field.setAvailable(available);
        }

        Field updatedField = fieldRepository.save(field);
        logger.info("Field updated successfully: {}", id);

        return updatedField;
    }

    /**
     * Récupère un terrain par son ID
     */
    @Transactional(readOnly = true)
    public Field getFieldById(UUID id) {
        logger.debug("Fetching field with ID: {}", id);

        return fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Field", id.toString()));
    }

    /**
     * Récupère tous les terrains avec pagination
     */
    @Transactional(readOnly = true)
    public Page<Field> getAllFields(Pageable pageable) {
        logger.debug("Fetching all fields with pagination");
        return fieldRepository.findAll(pageable);
    }

    /**
     * Récupère tous les terrains d'un établissement
     */
    @Transactional(readOnly = true)
    public List<Field> getFieldsByEstablishment(UUID establishmentId) {
        logger.debug("Fetching fields for establishment: {}", establishmentId);

        // Vérifier que l'établissement existe
        establishmentService.getEstablishmentById(establishmentId);

        return fieldRepository.findByEstablishmentId(establishmentId);
    }

    /**
     * Récupère tous les terrains disponibles
     */
    @Transactional(readOnly = true)
    public List<Field> getAvailableFields() {
        logger.debug("Fetching available fields");
        return fieldRepository.findByAvailableTrue();
    }

    /**
     * Supprime un terrain
     */
    public void deleteField(UUID id) {
        logger.info("Deleting field with ID: {}", id);

        Field field = getFieldById(id);

        // Vérification métier : pas de réservations actives
        // TODO: Ajouter cette vérification quand les réservations seront implémentées

        fieldRepository.delete(field);
        logger.info("Field deleted successfully: {}", id);
    }

    /**
     * Change la disponibilité d'un terrain
     */
    public Field toggleFieldAvailability(UUID id) {
        logger.info("Toggling availability for field: {}", id);

        Field field = getFieldById(id);
        field.setAvailable(!field.isAvailable());

        Field updatedField = fieldRepository.save(field);
        logger.info("Field availability toggled: {} - Available: {}", id, updatedField.isAvailable());

        return updatedField;
    }

    // Méthodes de validation privées

    private void validateFieldData(String name, String location, String surfaceType, int capacity) {
        validateName(name);
        validateLocation(location);
        validateSurfaceType(surfaceType);
        validateCapacity(capacity);
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessValidationException("Field name cannot be empty");
        }
        if (name.length() < 2 || name.length() > 100) {
            throw new BusinessValidationException("Field name must be between 2 and 100 characters");
        }
    }

    private void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new BusinessValidationException("Field location cannot be empty");
        }
        if (location.length() < 2 || location.length() > 255) {
            throw new BusinessValidationException("Field location must be between 2 and 255 characters");
        }
    }

    private void validateSurfaceType(String surfaceType) {
        if (surfaceType == null || surfaceType.trim().isEmpty()) {
            throw new BusinessValidationException("Surface type cannot be empty");
        }
        // Vous pouvez ajouter une validation avec des types autorisés
        List<String> allowedSurfaceTypes = List.of("Gazon naturel", "Gazon synthétique", "Terre battue", "Bitume");
        if (!allowedSurfaceTypes.contains(surfaceType)) {
            throw new BusinessValidationException("Invalid surface type. Allowed types: " +
                    String.join(", ", allowedSurfaceTypes));
        }
    }

    private void validateCapacity(int capacity) {
        if (capacity < 4 || capacity > 22) {
            throw new BusinessValidationException("Field capacity must be between 4 and 22 players");
        }
    }
}