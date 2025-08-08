package com.footArena.booking.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footArena.booking.api.dto.request.CreateEstablishmentRequest;
import com.footArena.booking.api.dto.response.EstablishmentResponse;
import com.footArena.booking.api.mappers.EstablishmentMapper;
import com.footArena.booking.config.TestSecurityConfig;
import com.footArena.booking.domain.entities.Establishment;
import com.footArena.booking.domain.services.EstablishmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EstablishmentController.class)
@Import(TestSecurityConfig.class) // IMPORT DE LA CONFIG TEST
class EstablishmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EstablishmentService establishmentService;

    @MockBean
    private EstablishmentMapper establishmentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllEstablishmentsNoPagination_ShouldReturnEstablishments() throws Exception {
        // Given
        List<Establishment> establishments = List.of(
                new Establishment("Test Establishment", "Test Address", "0123456789", "test@test.com")
        );

        List<EstablishmentResponse> responses = List.of(
                new EstablishmentResponse(UUID.randomUUID(), "Test Establishment", "Test Address",
                        "0123456789", "test@test.com", null, null)
        );

        when(establishmentService.getAllEstablishments()).thenReturn(establishments);
        when(establishmentMapper.toResponseList(establishments)).thenReturn(responses);

        // When & Then
        mockMvc.perform(get("/establishments/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Test Establishment"));
    }

    @Test
    void createEstablishment_WithValidData_ShouldReturnCreated() throws Exception {
        // Given
        CreateEstablishmentRequest request = new CreateEstablishmentRequest(
                "New Establishment", "New Address", "0123456789", "new@test.com"
        );

        Establishment savedEstablishment = new Establishment(
                request.getName(), request.getAddress(), request.getPhone(), request.getEmail()
        );

        EstablishmentResponse response = new EstablishmentResponse(
                UUID.randomUUID(), "New Establishment", "New Address",
                "0123456789", "new@test.com", null, null
        );

        when(establishmentService.createEstablishment(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(savedEstablishment);
        when(establishmentMapper.toResponse(savedEstablishment)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/establishments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Establishment"));
    }

    @Test
    void getEstablishmentById_WithValidId_ShouldReturnEstablishment() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        Establishment establishment = new Establishment("Test", "Address", "0123456789", "test@test.com");

        EstablishmentResponse response = new EstablishmentResponse(
                id, "Test", "Address", "0123456789", "test@test.com", null, null
        );

        when(establishmentService.getEstablishmentById(id)).thenReturn(establishment);
        when(establishmentMapper.toResponse(establishment)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/establishments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test"));
    }

    @Test
    void createEstablishment_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given
        CreateEstablishmentRequest invalidRequest = new CreateEstablishmentRequest(
                "", "", "invalid", "invalid-email" // Données invalides
        );

        // When & Then
        mockMvc.perform(post("/establishments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}