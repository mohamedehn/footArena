package com.footArena.booking.security.controllers;

import com.footArena.booking.domain.model.entity.User;
import com.footArena.booking.domain.model.enums.Role;
import com.footArena.booking.domain.repository.UserRepository;
import com.footArena.booking.security.services.BlackListTokenService;
import com.footArena.booking.security.services.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final BlackListTokenService blackListTokenService;

    public AuthController(UserRepository userRepository, AuthenticationManager authenticationManager,
                          TokenService tokenService, BlackListTokenService blackListTokenService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.blackListTokenService = blackListTokenService;
    }

    private void generateToken(String jwt, HttpServletResponse response, int expirationTime) {
        Cookie cookie = new Cookie("AuthToken", jwt);
        cookie.setHttpOnly(false); // TODO: Set to true for security in production, false for testing
        cookie.setPath("/");
        cookie.setMaxAge(expirationTime); // 1 day
        response.addCookie(cookie);
    }

    public boolean checkCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (nonNull(cookies)) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("AuthToken")) {
                    return true;
                }
            }
        }
        return false;
    }

    @GetMapping("/validate-token")
    public ResponseEntity<String> validateToken(HttpServletRequest request, @RequestHeader("Token") String token) {
        if (checkCookieToken(request)) {
            if (tokenService.isTokenValidAndNotExpired(token)) {
                return ResponseEntity.status(OK).body("Token is valid");
            }
        } else {
            return ResponseEntity.status(401).body("No token found in cookies");
        }
        return ResponseEntity.status(NOT_ACCEPTABLE).body("Token is invalid or expired");
    }

    @PostMapping("/register")
    @ResponseStatus(CREATED)
    public User registerUser(@RequestBody User user) {
        Role role = Role.PLAYER;

        if ((isNull(user.getFirstName()) || user.getFirstName().isEmpty()) ||
                (isNull(user.getLastName()) || user.getLastName().isEmpty()) || user.getPassword().length() < 6) {
            throw new ResponseStatusException(BAD_REQUEST, "First name and last name cannot be empty, and password must be at least 6 characters long");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(CONFLICT, "User with this email already exists");
        }

        PasswordEncoder passwordEncoder = createDelegatingPasswordEncoder();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(role);

        return this.userRepository.save(user);
    }

}