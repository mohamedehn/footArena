package com.footArena.booking.security.controllers;

import com.footArena.booking.api.dto.response.ApiResponse;
import com.footArena.booking.api.dto.response.UserResponse;
import com.footArena.booking.security.dto.AuthRequest;
import com.footArena.booking.security.dto.AuthResponse;
import com.footArena.booking.security.dto.RefreshTokenRequest;
import com.footArena.booking.security.services.AuthService;
import com.footArena.booking.security.services.RefreshTokenService;
import com.footArena.booking.security.services.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentification et gestion des sessions")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionService userSessionService;

    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          UserSessionService userSessionService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.userSessionService = userSessionService;
    }

    @Operation(summary = "Connexion utilisateur",
            description = "Authentifie un utilisateur et retourne des tokens d'accès")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        logger.info("Login attempt for email: {}", request.getEmail());

        try {
            AuthResponse authResponse = authService.authenticate(request, httpRequest);
            return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));

        } catch (Exception e) {
            logger.warn("Login failed for email: {} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Rafraîchir les tokens",
            description = "Utilise le refresh token pour obtenir de nouveaux tokens")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        logger.info("Token refresh attempt");

        try {
            AuthResponse authResponse = authService.refreshTokens(request, httpRequest);
            return ResponseEntity.ok(ApiResponse.success("Tokens refreshed successfully", authResponse));

        } catch (Exception e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token refresh failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Déconnexion",
            description = "Déconnecte l'utilisateur et révoque les tokens")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Refresh token à révoquer") @RequestParam(required = false) String refreshToken,
            HttpServletRequest request) {

        logger.info("Logout attempt");

        try {
            String accessToken = extractTokenFromRequest(request);
            authService.logout(accessToken, refreshToken);
            return ResponseEntity.ok(ApiResponse.success("Logout successful"));

        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success("Logout completed"));
        }
    }

    @Operation(summary = "Informations utilisateur courantes",
            description = "Récupère les informations de l'utilisateur connecté")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            HttpServletRequest request) {

        try {
            String token = extractTokenFromRequest(request);
            UserResponse user = authService.getUserFromToken(token);
            return ResponseEntity.ok(ApiResponse.success("User information retrieved", user));

        } catch (Exception e) {
            logger.debug("Failed to get current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Failed to retrieve user information"));
        }
    }

    @Operation(summary = "Valider un token",
            description = "Vérifie si un token d'accès est valide")
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<UserResponse>> validateToken(
            HttpServletRequest request) {

        try {
            String token = extractTokenFromRequest(request);

            if (!authService.isTokenValid(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Token is invalid or expired"));
            }

            UserResponse user = authService.getUserFromToken(token);
            return ResponseEntity.ok(ApiResponse.success("Token is valid", user));

        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token validation failed"));
        }
    }

    /**
     * Extrait le token Bearer de la requête
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("No valid token found in request");
    }
}