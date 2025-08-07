package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.Establishment;
import com.footArena.booking.domain.exceptions.BusinessValidationException;
import com.footArena.booking.domain.exceptions.ResourceNotFoundException;
import com.footArena.booking.domain.repositories.EstablishmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EstablishmentService {

    private static final Logger logger = LoggerFactory.getLogger(EstablishmentService.class);

    private final EstablishmentRepository establishmentRepository;

    public EstablishmentService(EstablishmentRepository establishmentRepository) {
        this.establishmentRepository = establishmentRepository;
    }

    /**
     * Crée un nouveau établissement
     */
    public Establishment createEstablishment(String name, String address, String phone, String email) {
        logger.info("Creating new establishment: {}", name);

        // Validation métier
        validateEstablishmentData(name, address, phone, email);
        checkEmailUniqueness(email);

        Establishment establishment = new Establishment(name, address, phone, email);

        Establishment savedEstablishment = establishmentRepository.save(establishment);
        logger.info("Establishment created with ID: {}", savedEstablishment.getId());

        return savedEstablishment;
    }

    /**
     * Met à jour un établissement existant
     */
    public Establishment updateEstablishment(UUID id, String name, String address, String phone, String email) {
        logger.info("Updating establishment with ID: {}", id);

        Establishment establishment = getEstablishmentById(id);

        // Validation métier si les champs sont fournis
        if (name != null) {
            validateName(name);
            establishment.setName(name);
        }

        if (address != null) {
            validateAddress(address);
            establishment.setAddress(address);
        }

        if (phone != null) {
            validatePhone(phone);
            establishment.setPhone(phone);
        }

        if (email != null) {
            validateEmail(email);
            if (!email.equals(establishment.getEmail())) {
                checkEmailUniqueness(email);
            }
            establishment.setEmail(email);
        }

        establishment.setUpdatedAt(new Date());

        Establishment updatedEstablishment = establishmentRepository.save(establishment);
        logger.info("Establishment updated successfully: {}", id);

        return updatedEstablishment;
    }

    /**
     * Récupère un établissement par son ID
     */
    @Transactional(readOnly = true)
    public Establishment getEstablishmentById(UUID id) {
        logger.debug("Fetching establishment with ID: {}", id);

        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Establishment", id.toString()));
    }

    /**
     * Récupère tous les établissements avec pagination
     */
    @Transactional(readOnly = true)
    public Page<Establishment> getAllEstablishments(Pageable pageable) {
        logger.debug("Fetching all establishments with pagination");
        return establishmentRepository.findAll(pageable);
    }

    /**
     * Récupère tous les établissements (sans pagination)
     */
    @Transactional(readOnly = true)
    public List<Establishment> getAllEstablishments() {
        logger.debug("Fetching all establishments");
        return establishmentRepository.findAll();
    }

    /**
     * Supprime un établissement
     */
    public void deleteEstablishment(UUID id) {
        logger.info("Deleting establishment with ID: {}", id);

        Establishment establishment = getEstablishmentById(id);

        // Vérification métier : pas de terrains actifs
        if (!establishment.getFields().isEmpty()) {
            throw new BusinessValidationException(
                    "Cannot delete establishment with active fields. Please delete all fields first.");
        }

        establishmentRepository.delete(establishment);
        logger.info("Establishment deleted successfully: {}", id);
    }

    /**
     * Vérifie si un établissement existe
     */
    @Transactional(readOnly = true)
    public boolean establishmentExists(UUID id) {
        return establishmentRepository.existsById(id);
    }

    // Méthodes de validation privées

    private void validateEstablishmentData(String name, String address, String phone, String email) {
        validateName(name);
        validateAddress(address);
        validatePhone(phone);
        validateEmail(email);
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessValidationException("Establishment name cannot be empty");
        }
        if (name.length() < 2 || name.length() > 100) {
            throw new BusinessValidationException("Establishment name must be between 2 and 100 characters");
        }
    }

    private void validateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new BusinessValidationException("Establishment address cannot be empty");
        }
        if (address.length() < 5 || address.length() > 255) {
            throw new BusinessValidationException("Establishment address must be between 5 and 255 characters");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^[0-9]{10}$")) {
            throw new BusinessValidationException("Phone number must contain exactly 10 digits");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BusinessValidationException("Invalid email format");
        }
    }

    private void checkEmailUniqueness(String email) {
        if (establishmentRepository.existsByEmail(email)) {
            throw new BusinessValidationException("An establishment with this email already exists");
        }
    }
}