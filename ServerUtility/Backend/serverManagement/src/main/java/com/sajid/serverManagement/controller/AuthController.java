package com.sajid.serverManagement.controller;

import com.sajid.serverManagement.dto.AuthResponse;
import com.sajid.serverManagement.dto.LoginRequest;
import com.sajid.serverManagement.dto.RegisterRequest;
import com.sajid.serverManagement.entity.User;
import com.sajid.serverManagement.service.JwtService;
import com.sajid.serverManagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

/**
 * Authentication Controller following REST API best practices
 * Handles login and registration endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    /**
     * Login endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for user: {}", loginRequest.getShortName());

            Optional<User> userOpt = userService.authenticateUser(
                loginRequest.getShortName(),
                loginRequest.getPassword()
            );

            if (userOpt.isEmpty()) {
                log.warn("Login failed for user: {}", loginRequest.getShortName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials", "INVALID_CREDENTIALS"));
            }

            User user = userOpt.get();
            String token = jwtService.generateToken(user.getShortName(), user.getRole().name());

            AuthResponse response = new AuthResponse(
                token,
                user.getShortName(),
                user.getFullName(),
                user.getRole().name(),
                jwtService.getExpirationTime()
            );

            log.info("Login successful for user: {} with role: {}", user.getShortName(), user.getRole());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Login error for user: {}", loginRequest.getShortName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Login failed", "LOGIN_ERROR"));
        }
    }

    /**
     * Registration endpoint
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            log.info("Registration attempt for user: {}", registerRequest.getShortName());

            if (userService.userExists(registerRequest.getShortName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("User already exists", "USER_EXISTS"));
            }

            User user = userService.registerUser(
                registerRequest.getShortName(),
                registerRequest.getFullName(),
                registerRequest.getPassword()
            );

            String token = jwtService.generateToken(user.getShortName(), user.getRole().name());

            AuthResponse response = new AuthResponse(
                token,
                user.getShortName(),
                user.getFullName(),
                user.getRole().name(),
                jwtService.getExpirationTime()
            );

            log.info("Registration successful for user: {}", user.getShortName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Registration error for user: {}", registerRequest.getShortName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Registration failed", "REGISTRATION_ERROR"));
        }
    }

    /**
     * Error response DTO for consistent error handling
     */
    public record ErrorResponse(String message, String code) {}
}
