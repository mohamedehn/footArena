package com.footArena.booking.domain.repository;

import com.footArena.booking.domain.model.entity.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {
    // Custom query methods can be defined here if needed
}