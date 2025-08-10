package com.footArena.booking.security.services;

import com.footArena.booking.domain.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private static final long ACCESS_TOKEN_VALIDITY = 15 * 60; // 15 minutes en secondes
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60; // 7 jours en secondes
    private static final long REMEMBER_ME_VALIDITY = 30 * 24 * 60 * 60; // 30 jours en secondes

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Génère un access token
     */
    public String generateAccessToken(User user) {
        return generateToken(user, ACCESS_TOKEN_VALIDITY, "access");
    }

    /**
     * Génère un refresh token
     */
    public String generateRefreshToken(User user, boolean rememberMe) {
        long validity = rememberMe ? REMEMBER_ME_VALIDITY : REFRESH_TOKEN_VALIDITY;
        return generateToken(user, validity, "refresh");
    }

    /**
     * Génère un token avec type spécifique
     */
    private String generateToken(User user, long validityInSeconds, String tokenType) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(validityInSeconds);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("tokenType", tokenType)
                .claim("emailVerified", user.isEmailVerified())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.error("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            logger.error("JWT signature validation failed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("JWT token compact of handler are invalid: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean isTokenValid(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return extractedUsername.equals(username) && !isTokenExpired(token);
        } catch (JwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "access".equals(tokenType);
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "refresh".equals(tokenType);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Convertit le rôle en authorities Spring Security
     */
    public Collection<? extends GrantedAuthority> getAuthorities(String token) {
        String role = extractRole(token);
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * Calcule le temps restant avant expiration (en secondes)
     */
    public long getTimeToExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);
            long now = System.currentTimeMillis();
            return Math.max(0, (expiration.getTime() - now) / 1000);
        } catch (JwtException e) {
            return 0;
        }
    }

    public LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Valide la structure du token sans vérifier l'expiration
     */
    public boolean isTokenStructureValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Token expiré mais structure valide
            return true;
        } catch (JwtException e) {
            logger.warn("Invalid token structure: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Génère un hash sécurisé d'un token pour stockage
     */
    public String hashToken(String token) {
        try {
            return String.valueOf(token.hashCode()); // Simplifié pour le dev
            // En production, utiliser un vrai hash cryptographique
        } catch (Exception e) {
            logger.error("Error hashing token", e);
            throw new RuntimeException("Token hashing failed");
        }
    }
}