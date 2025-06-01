package com.footArena.booking.security.services;

import com.footArena.booking.domain.model.entity.BlacklistedToken;
import com.footArena.booking.domain.model.entity.User;
import com.footArena.booking.domain.repository.BlacklistedTokenRepository;
import com.footArena.booking.domain.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.stream.Collectors;


import static java.util.Objects.nonNull;
import static java.time.temporal.ChronoUnit.HOURS;


@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final BlackListTokenService blackListTokenService;
    private final UserRepository userRepository;
    private final JwtDecoder jwtDecoder;

    public TokenService(JwtEncoder jwtEncoder, BlacklistedTokenRepository blacklistedTokenRepository,
                        BlackListTokenService blackListTokenService, UserRepository userRepository,
                        JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.blacklistedTokenRepository = blacklistedTokenRepository;
        this.blackListTokenService = blackListTokenService;
        this.userRepository = userRepository;
        this.jwtDecoder = jwtDecoder;
    }

    public String generateToken(Authentication auth) {
        UUID userId = findUserId(auth);

        BlacklistedToken blackListedToken = blacklistedTokenRepository.findByUserIdAndIsBlackListed(userId, false);
        if (nonNull(blackListedToken)) {
            if (blackListTokenService.isBlacklistTokenExpired(blackListedToken.getExpiresAt(), blackListedToken)) {
                String newToken = createAndEncodeJwt(auth);
                saveToken(newToken, userId);
            }
            return blackListedToken.getToken();
        }

        String jwtEncoded = createAndEncodeJwt(auth);
        saveToken(jwtEncoded, userId);
        return jwtEncoded;
    }

    private UUID findUserId(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName());
        if (nonNull(user)) {
            return user.getId();
        } else {
            throw new RuntimeException("User not found with provided credentials");
        }
    }


    private String createAndEncodeJwt(Authentication auth) {
        JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();

        JwtClaimsSet claims = createJwtClaimsSet(auth);

        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    private JwtClaimsSet createJwtClaimsSet(Authentication auth) {
        Instant now = Instant.now();
        Instant expirationDate = now.plusSeconds(3600); // Token valid for 1 hour

        String scope = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        return JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(expirationDate)
                .subject(auth.getName())
                .claim("email", String.valueOf((auth.getPrincipal())))
                .claim("scope", scope)
                .build();
    }

    private void saveToken(String token, UUID userId) {
        blackListTokenService.saveToken(token, Instant.now().plus(24, HOURS), userId);
    }

    private boolean validateToken(String token) {
        BlacklistedToken blackListedToken = blacklistedTokenRepository.findByToken(token);
        if (nonNull(blackListedToken) && !blackListedToken.isBlackListed()) {
            Date expirationDate = this.getTokenExpirationDate(String.valueOf(token));
            if (Instant.now().isAfter(expirationDate.toInstant()) || Instant.now().isAfter(blackListedToken.getExpiresAt())) {
                blackListedToken.setBlackListed(true);
                blacklistedTokenRepository.save(blackListedToken);
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isTokenValidAndNotExpired(String token) {
        return this.validateToken(token);
    }

    private Date getTokenExpirationDate(String token) {
        Jwt jwt = this.jwtDecoder.decode(token);
        Map<String, Object> claims = jwt.getClaims();
        Instant expInstant = (Instant) claims.get("exp");
        return new Date(expInstant.getEpochSecond() * 1000);
    }

}
