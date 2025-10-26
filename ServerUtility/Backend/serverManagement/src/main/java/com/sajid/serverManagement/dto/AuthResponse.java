package com.sajid.serverManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response DTO
 * Contains JWT token and user information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String shortName;
    private String fullName;
    private String role;
    private long expiresIn; // Token expiration in seconds
}
