package com.footArena.booking.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.footArena.booking.domain.model.entity.BlacklistedToken;

import java.util.UUID;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, UUID> {

}