package com.footArena.booking.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.footArena.booking.domain.model.entity.Match;

import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

}