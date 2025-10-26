package com.sajid.serverManagement.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User entity representing system users
 * Following Single Responsibility Principle - only handles user data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String shortName;      // Username/login identifier
    private String fullName;       // Display name
    private String password;       // BCrypt hashed password
    private Role role;            // User role (ADMIN/USER)

    public enum Role {
        ADMIN, USER
    }

    public User(String shortName, String fullName, String password) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.password = password;
        this.role = Role.USER; // Default role
    }
}
