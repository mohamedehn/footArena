package com.footArena.booking.api.controllers;

import com.footArena.booking.api.dto.request.CreateSlotRequest;
import com.footArena.booking.api.dto.request.UpdateSlotRequest;
import com.footArena.booking.api.dto.response.ApiResponse;
import com.footArena.booking.api.dto.response.PageResponse;
import com.footArena.booking.api.dto.response.SlotResponse;
import com.footArena.booking.api.mappers.SlotMapper;
import com.footArena.booking.domain.entities.Slot;
import com.footArena.booking.domain.enums.SlotStatus;
import com.footArena.booking.domain.services.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slots")
@Tag(name = "Slots", description = "Gestion des créneaux horaires")
public class SlotController {

    private static final Logger logger = LoggerFactory.getLogger(SlotController.class);

    private final SlotService slotService;
    private final SlotMapper slotMapper;

    public SlotController(SlotService slotService, SlotMapper slotMapper) {
        this.slotService = slotService;
        this.slotMapper = slotMapper;
    }

    @Operation(summary = "Créer un nouveau créneau",
            description = "Crée un nouveau créneau horaire pour un terrain")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Créneau créé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<SlotResponse>> createSlot(@Valid @RequestBody CreateSlotRequest request) {
        logger.info("Creating slot for field: {}", request.getFieldId());

        Slot slot = slotService.createSlot(
                request.getFieldId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getPrice(),
                request.getMaxCapacity(),
                request.getDescription()
        );

        SlotResponse response = slotMapper.toResponse(slot);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Créneau créé avec succès", response));
    }

    @Operation(summary = "Créer des créneaux récurrents",
            description = "Crée plusieurs créneaux selon un pattern récurrent")
    @PostMapping("/recurring")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> createRecurringSlots(
            @Parameter(description = "ID du terrain") @RequestParam UUID fieldId,
            @Parameter(description = "Date de début") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Heure de début") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @Parameter(description = "Heure de fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @Parameter(description = "Prix du créneau") @RequestParam BigDecimal price,
            @Parameter(description = "Capacité maximale") @RequestParam Integer maxCapacity,
            @Parameter(description = "Jours de la semaine (1=Lundi, 7=Dimanche)") @RequestParam List<Integer> daysOfWeek,
            @Parameter(description = "Description optionnelle") @RequestParam(required = false) String description) {

        logger.info("Creating recurring slots for field: {} from {} to {}", fieldId, startDate, endDate);

        List<Slot> slots = slotService.createRecurringSlots(
                fieldId, startDate, endDate, startTime, endTime,
                price, maxCapacity, daysOfWeek, description
        );

        List<SlotResponse> responses = slotMapper.toResponseList(slots);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(slots.size() + " créneaux créés avec succès", responses));
    }

    @Operation(summary = "Récupérer un créneau par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SlotResponse>> getSlotById(
            @Parameter(description = "ID du créneau") @PathVariable UUID id) {

        logger.debug("Fetching slot: {}", id);

        Slot slot = slotService.getSlotById(id);
        SlotResponse response = slotMapper.toResponse(slot);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Récupérer tous les créneaux avec pagination")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SlotResponse>>> getAllSlots(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "startTime") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "asc") String sortDir) {

        logger.debug("Fetching slots - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Slot> slots = slotService.getAllSlots(pageable);

        Page<SlotResponse> responsePage = slots.map(slotMapper::toResponse);
        PageResponse<SlotResponse> pageResponse = PageResponse.of(responsePage);

        return ResponseEntity.ok(ApiResponse.success("Créneaux récupérés avec succès", pageResponse));
    }

    @Operation(summary = "Récupérer les créneaux disponibles")
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> getAvailableSlots() {
        logger.debug("Fetching available slots");

        List<Slot> slots = slotService.getAvailableSlots();
        List<SlotResponse> responses = slotMapper.toResponseList(slots);

        return ResponseEntity.ok(ApiResponse.success("Créneaux disponibles récupérés", responses));
    }

    @Operation(summary = "Récupérer les créneaux d'un terrain")
    @GetMapping("/field/{fieldId}")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> getSlotsByField(
            @Parameter(description = "ID du terrain") @PathVariable UUID fieldId) {

        logger.debug("Fetching slots for field: {}", fieldId);

        List<Slot> slots = slotService.getSlotsByField(fieldId);
        List<SlotResponse> responses = slotMapper.toResponseList(slots);

        return ResponseEntity.ok(ApiResponse.success("Créneaux du terrain récupérés", responses));
    }

    @Operation(summary = "Rechercher des créneaux avec filtres")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<SlotResponse>>> searchSlots(
            @Parameter(description = "ID du terrain") @RequestParam(required = false) UUID fieldId,
            @Parameter(description = "Statut du créneau") @RequestParam(required = false) SlotStatus status,
            @Parameter(description = "Date de début") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Prix minimum") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Prix maximum") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Créneaux premium uniquement") @RequestParam(required = false) Boolean isPremium,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "startTime") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "asc") String sortDir) {

        logger.debug("Searching slots with filters");

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Slot> slots = slotService.searchSlots(fieldId, status, startDate, endDate,
                minPrice, maxPrice, isPremium, pageable);

        Page<SlotResponse> responsePage = slots.map(slotMapper::toResponse);
        PageResponse<SlotResponse> pageResponse = PageResponse.of(responsePage);

        return ResponseEntity.ok(ApiResponse.success("Recherche effectuée", pageResponse));
    }

    @Operation(summary = "Mettre à jour un créneau")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<SlotResponse>> updateSlot(
            @Parameter(description = "ID du créneau") @PathVariable UUID id,
            @Valid @RequestBody UpdateSlotRequest request) {

        logger.info("Updating slot: {}", id);

        Slot slot = slotService.updateSlot(
                id,
                request.getStartTime(),
                request.getEndTime(),
                request.getPrice(),
                request.getMaxCapacity(),
                request.getDescription()
        );

        SlotResponse response = slotMapper.toResponse(slot);
        return ResponseEntity.ok(ApiResponse.success("Créneau mis à jour avec succès", response));
    }

    @Operation(summary = "Changer le statut d'un créneau")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<SlotResponse>> changeSlotStatus(
            @Parameter(description = "ID du créneau") @PathVariable UUID id,
            @Parameter(description = "Nouveau statut") @RequestParam SlotStatus status) {

        logger.info("Changing slot {} status to {}", id, status);

        Slot slot = slotService.changeSlotStatus(id, status);
        SlotResponse response = slotMapper.toResponse(slot);

        return ResponseEntity.ok(ApiResponse.success("Statut du créneau modifié", response));
    }

    @Operation(summary = "Supprimer un créneau")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSlot(
            @Parameter(description = "ID du créneau") @PathVariable UUID id) {

        logger.info("Deleting slot: {}", id);

        slotService.deleteSlot(id);

        return ResponseEntity.ok(ApiResponse.success("Créneau supprimé avec succès"));
    }

    @Operation(summary = "Calculer le taux d'occupation d'un établissement")
    @GetMapping("/occupancy-rate/{establishmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Double>> getOccupancyRate(
            @Parameter(description = "ID de l'établissement") @PathVariable UUID establishmentId) {

        Double occupancyRate = slotService.calculateOccupancyRate(establishmentId);

        return ResponseEntity.ok(ApiResponse.success("Taux d'occupation calculé", occupancyRate));
    }
}