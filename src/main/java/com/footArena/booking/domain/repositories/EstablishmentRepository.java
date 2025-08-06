package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {
    // Custom query methods can be defined here if needed
}