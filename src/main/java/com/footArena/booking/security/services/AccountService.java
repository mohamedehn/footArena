package com.footArena.booking.security.services;

import com.footArena.booking.domain.entities.User;
import com.footArena.booking.domain.enums.Role;
import com.footArena.booking.domain.exceptions.BusinessValidationException;
import com.footArena.booking.domain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public AccountService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    /**
     * Crée un nouveau compte utilisateur
     */
    public User createAccount(String firstName, String lastName, String email,
                              String password, Role role) {
        logger.info("Creating new account for email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessValidationException("An account with this email already exists");
        }

        authService.validatePasswordStrength(password);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email.toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role != null ? role : Role.PLAYER);
        user.setEnabled(true); // En production, false jusqu'à vérification email

        user.setEmailVerificationToken(generateVerificationToken());

        User savedUser = userRepository.save(user);
        logger.info("Account created successfully for user: {}", savedUser.getId());

        // TODO: Envoyer email de vérification

        return savedUser;
    }

    /**
     * Active un compte avec le token de vérification
     */
    public void activateAccount(String verificationToken) {
        logger.info("Account activation attempt with token: {}", verificationToken);

        User user = userRepository.findByEmailVerificationToken(verificationToken)
                .orElseThrow(() -> new BusinessValidationException("Invalid verification token"));

        if (user.isEmailVerified()) {
            throw new BusinessValidationException("Account is already verified");
        }

        if (user.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new BusinessValidationException("Verification token has expired");
        }

        user.verifyEmail();
        user.setEnabled(true);
        userRepository.save(user);

        logger.info("Account activated successfully for user: {}", user.getEmail());
    }

    /**
     * Renvoie un email de vérification
     */
    public void resendVerificationEmail(String email) {
        logger.info("Resend verification email for: {}", email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BusinessValidationException("No account found with this email");
        }

        if (user.isEmailVerified()) {
            throw new BusinessValidationException("Account is already verified");
        }

        user.setEmailVerificationToken(generateVerificationToken());
        userRepository.save(user);

        // TODO: Envoyer email de vérification

        logger.info("Verification email sent for user: {}", user.getEmail());
    }

    /**
     * Désactive un compte
     */
    public void deactivateAccount(UUID userId, String reason) {
        logger.info("Deactivating account for user: {} - Reason: {}", userId, reason);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessValidationException("User not found"));

        user.setEnabled(false);
        userRepository.save(user);

        // Déconnecter de tous les appareils
        // authService.logoutFromAllDevices(userId);

        logger.info("Account deactivated for user: {}", userId);
    }

    /**
     * Supprime définitivement un compte
     */
    public void deleteAccount(UUID userId) {
        logger.info("Deleting account for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessValidationException("User not found"));

        // TODO: Supprimer toutes les données associées (RGPD)
        // - Refresh tokens
        // - User sessions
        // - Réservations
        // - etc.

        userRepository.delete(user);
        logger.info("Account deleted for user: {}", userId);
    }

    /**
     * Met à jour le profil utilisateur
     */
    public User updateProfile(UUID userId, String firstName, String lastName, String phoneNumber) {
        logger.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessValidationException("User not found"));

        if (firstName != null && !firstName.trim().isEmpty()) {
            user.setFirstName(firstName.trim());
        }

        if (lastName != null && !lastName.trim().isEmpty()) {
            user.setLastName(lastName.trim());
        }

        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber.trim());
        }

        User savedUser = userRepository.save(user);
        logger.info("Profile updated for user: {}", userId);

        return savedUser;
    }

    /**
     * Change l'email d'un utilisateur
     */
    public void changeEmail(UUID userId, String newEmail, String password) {
        logger.info("Email change request for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessValidationException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessValidationException("Incorrect password");
        }

        if (userRepository.existsByEmail(newEmail.toLowerCase())) {
            throw new BusinessValidationException("An account with this email already exists");
        }

        user.setEmail(newEmail.toLowerCase());
        user.setEmailVerifiedAt(null);
        user.setEmailVerificationToken(generateVerificationToken());

        userRepository.save(user);

        // TODO: Envoyer email de vérification au nouveau email

        logger.info("Email changed for user: {} to {}", userId, newEmail);
    }

    /**
     * Génère un token de vérification aléatoire
     */
    private String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Statistiques du compte
     */
    public AccountStats getAccountStats(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessValidationException("User not found"));

        return new AccountStats(
                user.isEnabled(),
                user.isEmailVerified(),
                user.getFailedLoginAttempts(),
                user.isAccountLocked(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    public static class AccountStats {
        private final boolean enabled;
        private final boolean emailVerified;
        private final int failedAttempts;
        private final boolean locked;
        private final LocalDateTime lastLogin;
        private final LocalDateTime createdAt;

        public AccountStats(boolean enabled, boolean emailVerified, int failedAttempts,
                            boolean locked, LocalDateTime lastLogin, LocalDateTime createdAt) {
            this.enabled = enabled;
            this.emailVerified = emailVerified;
            this.failedAttempts = failedAttempts;
            this.locked = locked;
            this.lastLogin = lastLogin;
            this.createdAt = createdAt;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isEmailVerified() {
            return emailVerified;
        }

        public int getFailedAttempts() {
            return failedAttempts;
        }

        public boolean isLocked() {
            return locked;
        }

        public LocalDateTime getLastLogin() {
            return lastLogin;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}