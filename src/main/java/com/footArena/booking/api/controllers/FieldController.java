package com.footArena.booking.api.controllers;

import com.footArena.booking.api.dto.response.ApiResponse;
import com.footArena.booking.api.dto.response.FieldResponse;
import com.footArena.booking.api.dto.response.PageResponse;
import com.footArena.booking.api.mappers.FieldMapper;
import com.footArena.booking.domain.entities.Field;
import com.footArena.booking.domain.services.FieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fields")
@Tag(name = "Fields", description = "Gestion des terrains")
public class FieldController {

    private static final Logger logger = LoggerFactory.getLogger(FieldController.class);

    private final FieldService fieldService;
    private final FieldMapper fieldMapper;

    public FieldController(FieldService fieldService, FieldMapper fieldMapper) {
        this.fieldService = fieldService;
        this.fieldMapper = fieldMapper;
    }

    @Operation(summary = "Créer un nouveau terrain")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FieldResponse>> createField(
            @Parameter(description = "Nom du terrain") @RequestParam @NotBlank String name,
            @Parameter(description = "Localisation") @RequestParam @NotBlank String location,
            @Parameter(description = "Type de surface") @RequestParam @NotBlank String surfaceType,
            @Parameter(description = "Capacité") @RequestParam @Min(4) @Max(22) Integer capacity,
            @Parameter(description = "Disponible") @RequestParam(defaultValue = "true") Boolean available,
            @Parameter(description = "ID de l'établissement") @RequestParam UUID establishmentId) {

        logger.info("Creating new field: {} for establishment: {}", name, establishmentId);

        Field field = fieldService.createField(name, location, surfaceType, capacity, available, establishmentId);
        FieldResponse response = fieldMapper.toResponse(field);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Terrain créé avec succès", response));
    }

    @Operation(summary = "Récupérer un terrain par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FieldResponse>> getFieldById(
            @Parameter(description = "ID du terrain") @PathVariable UUID id) {

        logger.debug("Fetching field with ID: {}", id);

        Field field = fieldService.getFieldById(id);
        FieldResponse response = fieldMapper.toResponse(field);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Récupérer tous les terrains avec pagination")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FieldResponse>>> getAllFields(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "asc") String sortDir) {

        logger.debug("Fetching fields - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Field> fields = fieldService.getAllFields(pageable);

        Page<FieldResponse> responsePage = fields.map(fieldMapper::toResponse);
        PageResponse<FieldResponse> pageResponse = PageResponse.of(responsePage);

        return ResponseEntity.ok(ApiResponse.success("Terrains récupérés avec succès", pageResponse));
    }

    @Operation(summary = "Récupérer les terrains d'un établissement")
    @GetMapping("/establishment/{establishmentId}")
    public ResponseEntity<ApiResponse<List<FieldResponse>>> getFieldsByEstablishment(
            @Parameter(description = "ID de l'établissement") @PathVariable UUID establishmentId) {

        logger.debug("Fetching fields for establishment: {}", establishmentId);

        List<Field> fields = fieldService.getFieldsByEstablishment(establishmentId);
        List<FieldResponse> responses = fieldMapper.toResponseList(fields);

        return ResponseEntity.ok(ApiResponse.success("Terrains récupérés avec succès", responses));
    }

    @Operation(summary = "Récupérer tous les terrains disponibles")
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<FieldResponse>>> getAvailableFields() {

        logger.debug("Fetching available fields");

        List<Field> fields = fieldService.getAvailableFields();
        List<FieldResponse> responses = fieldMapper.toResponseList(fields);

        return ResponseEntity.ok(ApiResponse.success("Terrains disponibles récupérés", responses));
    }

    @Operation(summary = "Mettre à jour un terrain")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FieldResponse>> updateField(
            @Parameter(description = "ID du terrain") @PathVariable UUID id,
            @Parameter(description = "Nom du terrain") @RequestParam(required = false) String name,
            @Parameter(description = "Localisation") @RequestParam(required = false) String location,
            @Parameter(description = "Type de surface") @RequestParam(required = false) String surfaceType,
            @Parameter(description = "Capacité") @RequestParam(required = false) Integer capacity,
            @Parameter(description = "Disponible") @RequestParam(required = false) Boolean available) {

        logger.info("Updating field with ID: {}", id);

        Field field = fieldService.updateField(id, name, location, surfaceType, capacity, available);
        FieldResponse response = fieldMapper.toResponse(field);

        return ResponseEntity.ok(ApiResponse.success("Terrain mis à jour avec succès", response));
    }

    @Operation(summary = "Changer la disponibilité d'un terrain")
    @PatchMapping("/{id}/toggle-availability")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FieldResponse>> toggleFieldAvailability(
            @Parameter(description = "ID du terrain") @PathVariable UUID id) {

        logger.info("Toggling availability for field: {}", id);

        Field field = fieldService.toggleFieldAvailability(id);
        FieldResponse response = fieldMapper.toResponse(field);

        return ResponseEntity.ok(ApiResponse.success("Disponibilité modifiée avec succès", response));
    }

    @Operation(summary = "Supprimer un terrain")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteField(
            @Parameter(description = "ID du terrain") @PathVariable UUID id) {

        logger.info("Deleting field with ID: {}", id);

        fieldService.deleteField(id);

        return ResponseEntity.ok(ApiResponse.success("Terrain supprimé avec succès"));
    }
}