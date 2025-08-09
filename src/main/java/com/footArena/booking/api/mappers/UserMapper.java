package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.response.UserResponse;
import com.footArena.booking.domain.entities.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    /**
     * Convertit un utilisateur en UserResponse
     *
     * @param user l'utilisateur à convertir
     * @return UserResponse contenant les informations de l'utilisateur
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getProfilePictureUrl(),
                user.getPhoneNumber(),
                user.getEmailVerifiedAt()
        );
    }

    /**
     * Convertit une liste d'utilisateurs en liste de UserResponse
     */
    public List<UserResponse> toResponseList(List<User> users) {
        if (users == null) {
            return List.of();
        }

        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit User en UserResponse sans informations sensibles
     */
    public UserResponse toPublicResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setEmailVerified(user.isEmailVerified());

        // Ne pas exposer l'email complet pour la sécurité
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            String[] parts = user.getEmail().split("@");
            String maskedEmail = parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
            response.setEmail(maskedEmail);
        }

        return response;
    }
}