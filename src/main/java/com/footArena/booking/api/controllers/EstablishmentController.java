package com.footArena.booking.api.controllers;

import com.footArena.booking.api.dto.request.CreateEstablishmentRequest;
import com.footArena.booking.api.dto.request.UpdateEstablishmentRequest;
import com.footArena.booking.api.dto.response.ApiResponse;
import com.footArena.booking.api.dto.response.EstablishmentResponse;
import com.footArena.booking.api.dto.response.PageResponse;
import com.footArena.booking.api.mappers.EstablishmentMapper;
import com.footArena.booking.domain.entities.Establishment;
import com.footArena.booking.domain.services.EstablishmentService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/establishments")
@Tag(name = "Establishments", description = "Gestion des établissements")
public class EstablishmentController {

    private static final Logger logger = LoggerFactory.getLogger(EstablishmentController.class);

    private final EstablishmentService establishmentService;
    private final EstablishmentMapper establishmentMapper;

    public EstablishmentController(EstablishmentService establishmentService,
                                   EstablishmentMapper establishmentMapper) {
        this.establishmentService = establishmentService;
        this.establishmentMapper = establishmentMapper;
    }

    @Operation(summary = "Créer un nouvel établissement",
            description = "Crée un nouvel établissement avec les informations fournies")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Établissement créé avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<EstablishmentResponse>> createEstablishment(
            @Valid @RequestBody CreateEstablishmentRequest request) {

        logger.info("Creating new establishment: {}", request.getName());

        Establishment establishment = establishmentService.createEstablishment(
                request.getName(),
                request.getAddress(),
                request.getPhone(),
                request.getEmail()
        );

        EstablishmentResponse response = establishmentMapper.toResponse(establishment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Établissement créé avec succès", response));
    }

    @Operation(summary = "Récupérer un établissement par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EstablishmentResponse>> getEstablishmentById(
            @Parameter(description = "ID de l'établissement") @PathVariable UUID id) {

        logger.debug("Fetching establishment with ID: {}", id);

        Establishment establishment = establishmentService.getEstablishmentById(id);
        EstablishmentResponse response = establishmentMapper.toResponse(establishment);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Récupérer tous les établissements avec pagination")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EstablishmentResponse>>> getAllEstablishments(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "asc") String sortDir) {

        logger.debug("Fetching establishments - page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Establishment> establishments = establishmentService.getAllEstablishments(pageable);

        // Conversion en réponses
        Page<EstablishmentResponse> responsePage = establishments
                .map(establishmentMapper::toResponse);

        PageResponse<EstablishmentResponse> pageResponse = PageResponse.of(responsePage);

        return ResponseEntity.ok(ApiResponse.success("Établissements récupérés avec succès", pageResponse));
    }

    @Operation(summary = "Récupérer tous les établissements (sans pagination)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EstablishmentResponse>>> getAllEstablishmentsNoPagination() {

        logger.debug("Fetching all establishments without pagination");

        List<Establishment> establishments = establishmentService.getAllEstablishments();
        List<EstablishmentResponse> responses = establishmentMapper.toResponseList(establishments);

        return ResponseEntity.ok(ApiResponse.success("Établissements récupérés avec succès", responses));
    }

    @Operation(summary = "Mettre à jour un établissement")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<EstablishmentResponse>> updateEstablishment(
            @Parameter(description = "ID de l'établissement") @PathVariable UUID id,
            @Valid @RequestBody UpdateEstablishmentRequest request) {

        logger.info("Updating establishment with ID: {}", id);

        Establishment establishment = establishmentService.updateEstablishment(
                id,
                request.getName(),
                request.getAddress(),
                request.getPhone(),
                request.getEmail()
        );

        EstablishmentResponse response = establishmentMapper.toResponse(establishment);

        return ResponseEntity.ok(ApiResponse.success("Établissement mis à jour avec succès", response));
    }

    @Operation(summary = "Supprimer un établissement")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEstablishment(
            @Parameter(description = "ID de l'établissement") @PathVariable UUID id) {

        logger.info("Deleting establishment with ID: {}", id);

        establishmentService.deleteEstablishment(id);

        return ResponseEntity.ok(ApiResponse.success("Établissement supprimé avec succès"));
    }

    @Operation(summary = "Vérifier l'existence d'un établissement")
    @GetMapping("/{id}/exists")
    public ResponseEntity<ApiResponse<Boolean>> establishmentExists(
            @Parameter(description = "ID de l'établissement") @PathVariable UUID id) {

        boolean exists = establishmentService.establishmentExists(id);

        return ResponseEntity.ok(ApiResponse.success("Vérification effectuée", exists));
    }
}