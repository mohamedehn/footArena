package com.footArena.booking.domain.repository;

import com.footArena.booking.domain.model.entity.MatchPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, UUID> {
    // Custom query methods can be defined here if needed
}