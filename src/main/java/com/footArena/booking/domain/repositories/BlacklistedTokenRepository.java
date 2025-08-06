package com.footArena.booking.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.footArena.booking.domain.entities.BlacklistedToken;

import java.util.UUID;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, UUID> {
    public BlacklistedToken findByUserIdAndIsBlackListed(UUID userId, boolean isBlackListed);

    public BlacklistedToken findByToken(String token);
}