package com.footArena.booking.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.footArena.booking.domain.entities.Match;

import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

}