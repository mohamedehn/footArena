package com.footArena.booking.api.controllers;

import com.footArena.booking.api.dto.request.CreateUserRequest;
import com.footArena.booking.api.dto.request.UpdateProfileRequest;
import com.footArena.booking.api.dto.response.ApiResponse;
import com.footArena.booking.api.dto.response.UserResponse;
import com.footArena.booking.api.mappers.UserMapper;
import com.footArena.booking.domain.entities.User;
import com.footArena.booking.domain.enums.Role;
import com.footArena.booking.security.services.AccountService;
import com.footArena.booking.security.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/account")
@Tag(name = "Account Management", description = "Gestion des comptes utilisateurs")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;
    private final AuthService authService;
    private final UserMapper userMapper;

    public AccountController(AccountService accountService,
                             AuthService authService,
                             UserMapper userMapper) {
        this.accountService = accountService;
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @Operation(summary = "Créer un nouveau compte",
            description = "Crée un nouveau compte utilisateur")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody CreateUserRequest request) {

        logger.info("Registration attempt for email: {}", request.getEmail());

        try {
            User user = accountService.createAccount(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPassword(),
                    Role.PLAYER
            );

            UserResponse userResponse = userMapper.toResponse(user);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Account created successfully. Please check your email for verification.", userResponse));

        } catch (Exception e) {
            logger.warn("Registration failed for email: {} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Activer un compte",
            description = "Active un compte avec le token de vérification")
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activateAccount(
            @Parameter(description = "Token de vérification") @RequestParam String token) {

        logger.info("Account activation attempt");

        try {
            accountService.activateAccount(token);
            return ResponseEntity.ok(ApiResponse.success("Account activated successfully"));

        } catch (Exception e) {
            logger.warn("Account activation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Account activation failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Renvoyer l'email de vérification",
            description = "Renvoie l'email de vérification")
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @Parameter(description = "Adresse email") @RequestParam String email) {

        logger.info("Resend verification email for: {}", email);

        try {
            accountService.resendVerificationEmail(email);
            return ResponseEntity.ok(ApiResponse.success("Verification email sent"));

        } catch (Exception e) {
            logger.warn("Resend verification failed for email: {} - {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to send verification email: " + e.getMessage()));
        }
    }

    @Operation(summary = "Mettre à jour le profil",
            description = "Met à jour les informations du profil utilisateur")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {

        logger.info("Profile update attempt");

        try {
            String token = extractTokenFromRequest(httpRequest);
            UserResponse currentUser = authService.getUserFromToken(token);

            User updatedUser = accountService.updateProfile(
                    currentUser.getId(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhoneNumber()
            );

            UserResponse userResponse = userMapper.toResponse(updatedUser);

            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userResponse));

        } catch (Exception e) {
            logger.warn("Profile update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Profile update failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Changer l'email",
            description = "Change l'adresse email du compte")
    @PostMapping("/change-email")
    public ResponseEntity<ApiResponse<Void>> changeEmail(
            @Parameter(description = "Nouvelle adresse email") @RequestParam String newEmail,
            @Parameter(description = "Mot de passe de confirmation") @RequestParam String password,
            HttpServletRequest httpRequest) {

        logger.info("Email change attempt");

        try {
            String token = extractTokenFromRequest(httpRequest);
            UserResponse currentUser = authService.getUserFromToken(token);

            accountService.changeEmail(currentUser.getId(), newEmail, password);

            return ResponseEntity.ok(ApiResponse.success("Email changed successfully. Please verify your new email."));

        } catch (Exception e) {
            logger.warn("Email change failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Email change failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Désactiver le compte",
            description = "Désactive le compte utilisateur")
    @PostMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @Parameter(description = "Raison de la désactivation") @RequestParam(required = false) String reason,
            HttpServletRequest httpRequest) {

        logger.info("Account deactivation attempt");

        try {
            String token = extractTokenFromRequest(httpRequest);
            UserResponse currentUser = authService.getUserFromToken(token);

            accountService.deactivateAccount(currentUser.getId(), reason);
            authService.logoutFromAllDevices(currentUser.getId());

            return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully"));

        } catch (Exception e) {
            logger.warn("Account deactivation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Account deactivation failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Supprimer le compte",
            description = "Supprime définitivement le compte (RGPD)")
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Parameter(description = "Mot de passe de confirmation") @RequestParam String password,
            HttpServletRequest httpRequest) {

        logger.info("Account deletion attempt");

        try {
            String token = extractTokenFromRequest(httpRequest);
            UserResponse currentUser = authService.getUserFromToken(token);

            // Vérifier le mot de passe avant suppression
            // authService.validatePassword(currentUser.getId(), password);

            accountService.deleteAccount(currentUser.getId());

            return ResponseEntity.ok(ApiResponse.success("Account deleted successfully"));

        } catch (Exception e) {
            logger.warn("Account deletion failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Account deletion failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Statistiques du compte",
            description = "Récupère les statistiques du compte utilisateur")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AccountService.AccountStats>> getAccountStats(
            HttpServletRequest httpRequest) {

        try {
            String token = extractTokenFromRequest(httpRequest);
            UserResponse currentUser = authService.getUserFromToken(token);

            AccountService.AccountStats stats = accountService.getAccountStats(currentUser.getId());

            return ResponseEntity.ok(ApiResponse.success("Account statistics retrieved", stats));

        } catch (Exception e) {
            logger.error("Failed to get account stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve account statistics"));
        }
    }

    @Operation(summary = "Créer un compte admin",
            description = "Crée un compte administrateur (admin uniquement)")
    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createAdminAccount(
            @Valid @RequestBody CreateUserRequest request) {

        logger.info("Admin account creation attempt");

        try {
            User user = accountService.createAccount(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPassword(),
                    Role.ADMIN
            );

            UserResponse userResponse = userMapper.toResponse(user);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Admin account created successfully", userResponse));

        } catch (Exception e) {
            logger.warn("Admin account creation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Admin account creation failed: " + e.getMessage()));
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