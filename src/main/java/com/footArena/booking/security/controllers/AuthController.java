package com.footArena.booking.security.controllers;

import com.footArena.booking.domain.entities.User;
import com.footArena.booking.domain.enums.Role;
import com.footArena.booking.domain.repositories.UserRepository;
import com.footArena.booking.security.dto.LoginDTO;
import com.footArena.booking.security.services.BlackListTokenService;
import com.footArena.booking.security.services.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final BlackListTokenService blackListTokenService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);


    public AuthController(UserRepository userRepository, AuthenticationManager authenticationManager,
                          TokenService tokenService, BlackListTokenService blackListTokenService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.blackListTokenService = blackListTokenService;
    }

    private void generateCookie(String jwt, HttpServletResponse response) {
        Cookie cookie = new Cookie("AuthToken", jwt);
        cookie.setHttpOnly(false); // TODO: Set to true for security in production, false for testing
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 1 day in seconds
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
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        Role role = Role.PLAYER;

        if ((isNull(user.getFirstName()) || user.getFirstName().isEmpty()) ||
                (isNull(user.getLastName()) || user.getLastName().isEmpty()) || user.getPassword().length() < 6) {
            throw new ResponseStatusException(BAD_REQUEST, "You must provide all required fields.");
        }

        if (isNull(user.getEmail()) || user.getEmail().isEmpty() || isNull(user.getPassword()) || user.getPassword().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email and password cannot be empty");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(CONFLICT, "User with this email already exists");
        }

        System.out.println("here");

        PasswordEncoder passwordEncoder = createDelegatingPasswordEncoder();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        user.setRole(role);

        this.userRepository.save(user);

        return new ResponseEntity<>("User registered successfully", CREATED);
    }

    @PostMapping("/login")
    @ResponseStatus(OK)
    public ResponseEntity<?> authenticateUser(@RequestBody LoginDTO loginDTO, HttpServletResponse response, HttpServletRequest request) {
        if (isNull(loginDTO.getEmail()) || isNull(loginDTO.getPassword())) {
            throw new ResponseStatusException(BAD_REQUEST, "Email and password cannot be empty");
        }
        if(checkCookieToken(request)) {
            tokenService.isTokenValidAndNotExpired(request.getHeader("AuthToken"));
        }

        try {
            Authentication authentication = this.authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword()));

            var jwt = tokenService.generateToken(authentication);
            generateCookie(jwt, response);
            getContext().setAuthentication(authentication);

            User user = this.userRepository.findByEmail(loginDTO.getEmail());
            if (isNull(user)) {
                throw new ResponseStatusException(UNAUTHORIZED, "User not found with provided credentials");
            }

            return new ResponseEntity<>("User logged in successfully", OK);

        } catch (Exception ex) {
            log.warn("Authentication failed for user: {}", loginDTO.getEmail(), ex);
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }
    }

}