package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.Field;
import com.footArena.booking.domain.entities.Slot;
import com.footArena.booking.domain.enums.SlotStatus;
import com.footArena.booking.domain.exceptions.BusinessValidationException;
import com.footArena.booking.domain.exceptions.ResourceNotFoundException;
import com.footArena.booking.domain.repositories.SlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SlotService {

    private static final Logger logger = LoggerFactory.getLogger(SlotService.class);

    private final SlotRepository slotRepository;
    private final FieldService fieldService;

    public SlotService(SlotRepository slotRepository, FieldService fieldService) {
        this.slotRepository = slotRepository;
        this.fieldService = fieldService;
    }

    /**
     * Crée un nouveau créneau
     */
    public Slot createSlot(UUID fieldId, LocalDateTime startTime, LocalDateTime endTime,
                           BigDecimal price, Integer maxCapacity, String description) {
        logger.info("Creating slot for field: {} from {} to {}", fieldId, startTime, endTime);

        Field field = fieldService.getFieldById(fieldId);

        // Validations métier
        validateSlotData(startTime, endTime, price, maxCapacity);
        checkTimeConflicts(fieldId, startTime, endTime, null);

        Slot slot = new Slot(field, startTime, endTime, price, maxCapacity);
        slot.setDescription(description);

        Slot savedSlot = slotRepository.save(slot);
        logger.info("Slot created with ID: {}", savedSlot.getId());

        return savedSlot;
    }

    /**
     * Crée des créneaux récurrents
     */
    public List<Slot> createRecurringSlots(UUID fieldId, LocalDateTime startDate, LocalDateTime endDate,
                                           LocalTime startTime, LocalTime endTime, BigDecimal price,
                                           Integer maxCapacity, List<Integer> daysOfWeek, String description) {
        logger.info("Creating recurring slots for field: {} from {} to {}", fieldId, startDate, endDate);

        Field field = fieldService.getFieldById(fieldId);
        List<Slot> createdSlots = new ArrayList<>();

        LocalDateTime currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (daysOfWeek.contains(currentDate.getDayOfWeek().getValue())) {
                LocalDateTime slotStart = currentDate.toLocalDate().atTime(startTime);
                LocalDateTime slotEnd = currentDate.toLocalDate().atTime(endTime);

                // Vérifier les conflits pour chaque créneau
                List<Slot> conflicts = slotRepository.findConflictingSlots(fieldId, slotStart, slotEnd);
                if (conflicts.isEmpty()) {
                    Slot slot = new Slot(field, slotStart, slotEnd, price, maxCapacity);
                    slot.setDescription(description);
                    slot.setRecurringPattern("WEEKLY");

                    createdSlots.add(slotRepository.save(slot));
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        logger.info("Created {} recurring slots", createdSlots.size());
        return createdSlots;
    }

    /**
     * Met à jour un créneau
     */
    public Slot updateSlot(UUID slotId, LocalDateTime startTime, LocalDateTime endTime,
                           BigDecimal price, Integer maxCapacity, String description) {
        logger.info("Updating slot: {}", slotId);

        Slot slot = getSlotById(slotId);

        // Vérifier si le créneau peut être modifié
        if (slot.getCurrentBookings() > 0 && !slot.canBeCancelled()) {
            throw new BusinessValidationException("Cannot modify slot with confirmed bookings too close to start time");
        }

        if (startTime != null && endTime != null) {
            validateSlotData(startTime, endTime, price != null ? price : slot.getPrice(),
                    maxCapacity != null ? maxCapacity : slot.getMaxCapacity());
            checkTimeConflicts(slot.getField().getId(), startTime, endTime, slotId);
            slot.setStartTime(startTime);
            slot.setEndTime(endTime);
        }

        if (price != null) {
            slot.setPrice(price);
        }

        if (maxCapacity != null) {
            if (maxCapacity < slot.getCurrentBookings()) {
                throw new BusinessValidationException("Cannot reduce capacity below current bookings count");
            }
            slot.setMaxCapacity(maxCapacity);
        }

        if (description != null) {
            slot.setDescription(description);
        }

        Slot updatedSlot = slotRepository.save(slot);
        logger.info("Slot updated successfully: {}", slotId);

        return updatedSlot;
    }

    /**
     * Récupère un créneau par ID
     */
    @Transactional(readOnly = true)
    public Slot getSlotById(UUID slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", slotId.toString()));
    }

    /**
     * Récupère tous les créneaux avec pagination
     */
    @Transactional(readOnly = true)
    public Page<Slot> getAllSlots(Pageable pageable) {
        return slotRepository.findAll(pageable);
    }

    /**
     * Récupère les créneaux d'un terrain
     */
    @Transactional(readOnly = true)
    public List<Slot> getSlotsByField(UUID fieldId) {
        return slotRepository.findByFieldId(fieldId);
    }

    /**
     * Récupère les créneaux disponibles
     */
    @Transactional(readOnly = true)
    public List<Slot> getAvailableSlots() {
        return slotRepository.findByStatus(SlotStatus.AVAILABLE);
    }

    /**
     * Récupère les créneaux disponibles dans une plage de dates
     */
    @Transactional(readOnly = true)
    public List<Slot> getAvailableSlotsBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return slotRepository.findAvailableSlotsBetween(SlotStatus.AVAILABLE, startTime, endTime);
    }

    /**
     * Recherche de créneaux avec filtres
     */
    @Transactional(readOnly = true)
    public Page<Slot> searchSlots(UUID fieldId, SlotStatus status, LocalDateTime startDate,
                                  LocalDateTime endDate, BigDecimal minPrice, BigDecimal maxPrice,
                                  Boolean isPremium, Pageable pageable) {
        return slotRepository.findSlotsWithFilters(fieldId, status, startDate, endDate,
                minPrice, maxPrice, isPremium, pageable);
    }

    /**
     * Change le statut d'un créneau
     */
    public Slot changeSlotStatus(UUID slotId, SlotStatus newStatus) {
        logger.info("Changing slot {} status to {}", slotId, newStatus);

        Slot slot = getSlotById(slotId);

        // Validations métier selon le nouveau statut
        if (newStatus == SlotStatus.CANCELLED && slot.getCurrentBookings() > 0) {
            throw new BusinessValidationException("Cannot cancel slot with existing bookings");
        }

        slot.setStatus(newStatus);

        Slot updatedSlot = slotRepository.save(slot);
        logger.info("Slot status changed successfully");

        return updatedSlot;
    }

    public void deleteSlot(UUID slotId) {
        logger.info("Deleting slot: {}", slotId);

        Slot slot = getSlotById(slotId);

        if (slot.getCurrentBookings() > 0) {
            throw new BusinessValidationException("Cannot delete slot with existing bookings");
        }

        slotRepository.delete(slot);
        logger.info("Slot deleted successfully: {}", slotId);
    }

    /**
     * Calcule le taux d'occupation d'un établissement
     */
    @Transactional(readOnly = true)
    public Double calculateOccupancyRate(UUID establishmentId) {
        return slotRepository.calculateOccupancyRateByEstablishment(establishmentId);
    }

    @Transactional
    public void cleanupExpiredSlots() {
        logger.info("Starting cleanup of expired slots");

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(1);
        List<Slot> expiredSlots = slotRepository.findExpiredSlots(cutoffTime);

        for (Slot slot : expiredSlots) {
            if (slot.getCurrentBookings() == 0) {
                slotRepository.delete(slot);
            }
        }

        logger.info("Cleanup completed. Processed {} expired slots", expiredSlots.size());
    }

    private void validateSlotData(LocalDateTime startTime, LocalDateTime endTime,
                                  BigDecimal price, Integer maxCapacity) {
        if (startTime == null || endTime == null) {
            throw new BusinessValidationException("Start time and end time are required");
        }

        if (startTime.isAfter(endTime)) {
            throw new BusinessValidationException("Start time must be before end time");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("Cannot create slot in the past");
        }

        long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes < 30) {
            throw new BusinessValidationException("Slot duration must be at least 30 minutes");
        }

        if (durationMinutes > 480) { // 8 heures
            throw new BusinessValidationException("Slot duration cannot exceed 8 hours");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException("Price must be positive");
        }

        if (maxCapacity == null || maxCapacity < 1) {
            throw new BusinessValidationException("Max capacity must be at least 1");
        }
    }

    private void checkTimeConflicts(UUID fieldId, LocalDateTime startTime,
                                    LocalDateTime endTime, UUID excludeSlotId) {
        List<Slot> conflictingSlots = slotRepository.findConflictingSlots(fieldId, startTime, endTime);

        if (excludeSlotId != null) {
            conflictingSlots.removeIf(slot -> slot.getId().equals(excludeSlotId));
        }

        if (!conflictingSlots.isEmpty()) {
            throw new BusinessValidationException("Time slot conflicts with existing slot(s)");
        }
    }
}