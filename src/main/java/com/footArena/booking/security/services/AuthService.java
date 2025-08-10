package com.footArena.booking.security.services;

import com.footArena.booking.api.dto.response.UserResponse;
import com.footArena.booking.api.mappers.UserMapper;
import com.footArena.booking.domain.entities.User;
import com.footArena.booking.domain.exceptions.BusinessValidationException;
import com.footArena.booking.domain.exceptions.UnauthorizedAccessException;
import com.footArena.booking.domain.repositories.UserRepository;
import com.footArena.booking.security.dto.AuthRequest;
import com.footArena.booking.security.dto.AuthResponse;
import com.footArena.booking.security.dto.RefreshTokenRequest;
import com.footArena.booking.security.entities.RefreshToken;
import com.footArena.booking.security.entities.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionService userSessionService;
    private final UserMapper userMapper;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserSessionService userSessionService,
                       UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userSessionService = userSessionService;
        this.userMapper = userMapper;
    }

    /**
     * Authentification complète avec dual tokens
     */
    public AuthResponse authenticate(AuthRequest request, HttpServletRequest httpRequest) {
        logger.info("Authentication attempt for email: {}", request.getEmail());

        try {
            // check if user exists and is enabled
            User user = validateUserForLogin(request.getEmail());

            // check if connexion is suspicious
            if (userSessionService.detectSuspiciousLogin(user, httpRequest)) {
                logger.warn("Suspicious login detected for user: {}", user.getEmail());
                // TODO: envoyer un email d'alerte
            }

            // Auth Spring Security
            Authentication authentication = authenticateUser(request);
            resetFailedAttempts(user);

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user, request.isRememberMe());

            // save refresh token
            RefreshToken savedRefreshToken = refreshTokenService.createRefreshToken(
                    user,
                    refreshToken,
                    request.isRememberMe(),
                    request.getDeviceInfo(),
                    getClientIpAddress(httpRequest)
            );


            UserSession session = userSessionService.createSession(user, accessToken, httpRequest);
            user.updateLastLogin();
            userRepository.save(user);

            // response
            UserResponse userResponse = userMapper.toResponse(user);
            AuthResponse authResponse = new AuthResponse(
                    accessToken,
                    refreshToken,
                    jwtService.getTimeToExpiration(accessToken),
                    userResponse
            );

            logger.info("Authentication successful for user: {}", user.getEmail());
            return authResponse;

        } catch (AuthenticationException e) {
            handleFailedAuthentication(request.getEmail(), e);
            throw new UnauthorizedAccessException("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Rafraîchit les tokens en utilisant le refresh token
     */
    public AuthResponse refreshTokens(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        logger.info("Token refresh attempt");

        try {
            // Valider le refresh token JWT
            if (!jwtService.isRefreshToken(request.getRefreshToken()) ||
                    jwtService.isTokenExpired(request.getRefreshToken())) {
                throw new UnauthorizedAccessException("Invalid or expired refresh token");
            }

            // Valider et utiliser le refresh token en base
            RefreshToken refreshToken = refreshTokenService.validateAndUseRefreshToken(request.getRefreshToken());
            User user = refreshToken.getUser();

            // Vérifier si l'utilisateur est toujours actif
            if (!user.isEnabled()) {
                throw new UnauthorizedAccessException("User account is disabled");
            }

            // Détecter une activité suspecte
            String currentIp = getClientIpAddress(httpRequest);
            if (refreshTokenService.detectSuspiciousActivity(refreshToken, currentIp)) {
                logger.warn("Suspicious refresh token activity detected for user: {}", user.getEmail());
                refreshTokenService.revokeRefreshToken(request.getRefreshToken());
                throw new UnauthorizedAccessException("Suspicious activity detected. Please login again.");
            }

            // Générer de nouveaux tokens
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user, false);

            // Rotation du refresh token (sécurité)
            RefreshToken newRefreshTokenEntity = refreshTokenService.rotateRefreshToken(
                    refreshToken,
                    newRefreshToken,
                    request.getDeviceInfo(),
                    currentIp
            );

            // Mettre à jour la session
            userSessionService.updateSessionActivity(newAccessToken);

            // Response
            UserResponse userResponse = userMapper.toResponse(user);
            AuthResponse authResponse = new AuthResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtService.getTimeToExpiration(newAccessToken),
                    userResponse
            );

            logger.info("Token refresh successful for user: {}", user.getEmail());
            return authResponse;

        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            throw new UnauthorizedAccessException("Token refresh failed");
        }
    }

    /**
     * Déconnexion complète avec révocation des tokens
     */
    public void logout(String accessToken, String refreshToken) {
        logger.info("Logout attempt");

        try {
            UUID userId = jwtService.extractUserId(accessToken);

            if (userId != null) {
                // Révoquer le refresh token spécifique
                if (refreshToken != null) {
                    refreshTokenService.revokeRefreshToken(refreshToken);
                }

                userSessionService.deactivateSession(accessToken);

                // Ajouter l'access token à la blacklist
                // TODO: Implémenter blacklist des access tokens

                logger.info("Logout successful for user ID: {}", userId);
            }
        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage());
            // Ne pas faire échouer la déconnexion même en cas d'erreur
        }
    }

    /**
     * Déconnexion de tous les appareils
     */
    public void logoutFromAllDevices(UUID userId) {
        logger.info("Logout from all devices for user: {}", userId);

        refreshTokenService.revokeAllUserTokens(userId);
        userSessionService.deactivateAllUserSessions(userId);

        logger.info("Logout from all devices successful for user: {}", userId);
    }

    /**
     * Valide un utilisateur pour la connexion
     */
    private User validateUserForLogin(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UnauthorizedAccessException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        if (user.isAccountLocked()) {
            throw new UnauthorizedAccessException("Account is temporarily locked due to too many failed attempts");
        }

        return user;
    }

    /**
     * Authentifie l'utilisateur avec Spring Security
     */
    private Authentication authenticateUser(AuthRequest request) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new UnauthorizedAccessException("Invalid email or password");
        } catch (DisabledException e) {
            throw new UnauthorizedAccessException("Account is disabled");
        }
    }

    /**
     * Gère les échecs d'authentification
     */
    private void handleFailedAuthentication(String email, AuthenticationException e) {
        logger.warn("Authentication failed for email: {} - {}", email, e.getMessage());

        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.incrementFailedAttempts();

            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.lockAccount(LOCKOUT_DURATION_MINUTES);
                logger.warn("Account locked for user: {} due to {} failed attempts",
                        email, user.getFailedLoginAttempts());
            }

            userRepository.save(user);
        }
    }

    /**
     * Réinitialise les tentatives échouées après connexion réussie
     */
    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0) {
            user.resetFailedAttempts();
            user.unlockAccount();
            userRepository.save(user);
        }
    }

    /**
     * Extrait l'adresse IP du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Valide la force d'un mot de passe
     */
    public void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessValidationException("Password must be at least 8 characters long");
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new BusinessValidationException(
                    "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
            );
        }
    }

    /**
     * Change le mot de passe d'un utilisateur
     */
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        logger.info("Password change request for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UnauthorizedAccessException("Current password is incorrect");
        }

        validatePasswordStrength(newPassword);

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessValidationException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logoutFromAllDevices(userId);

        logger.info("Password changed successfully for user: {}", userId);
    }

    /**
     * Vérifie si un token est valide
     */
    public boolean isTokenValid(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return false;
            }

            if (!jwtService.isTokenStructureValid(token) || jwtService.isTokenExpired(token)) {
                return false;
            }

            if (!jwtService.isAccessToken(token)) {
                return false;
            }

            // TODO: Vérifier si le token n'est pas blacklisté

            return true;
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Récupère les informations utilisateur depuis un token
     */
    public UserResponse getUserFromToken(String token) {
        if (!isTokenValid(token)) {
            throw new UnauthorizedAccessException("Invalid token");
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found"));

        return userMapper.toResponse(user);
    }
}