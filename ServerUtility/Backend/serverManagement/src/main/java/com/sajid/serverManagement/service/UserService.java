package com.sajid.serverManagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sajid.serverManagement.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * User service following Repository pattern with single JSON file storage
 * Handles user CRUD operations and authentication logic
 */
@Slf4j
@Service
public class UserService {

    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Path usersFilePath;

    public UserService(@Value("${app.users.directory:./data}") String usersDir) {
        this.objectMapper = new ObjectMapper();
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.usersFilePath = Paths.get(usersDir, "users.json");
    }

    /**
     * Initialize users file and create default admin users
     */
    @PostConstruct
    public void initializeUsers() {
        try {
            // Create users directory if it doesn't exist
            Path usersDirectory = usersFilePath.getParent();
            if (!Files.exists(usersDirectory)) {
                Files.createDirectories(usersDirectory);
                log.info("Created users directory: {}", usersDirectory.toAbsolutePath());
            }

            // Initialize users file if it doesn't exist
            if (!Files.exists(usersFilePath)) {
                saveAllUsers(new ArrayList<>());
                log.info("Created users file: {}", usersFilePath.toAbsolutePath());
            }

            // Create default admin users
            createDefaultAdminUsers();

        } catch (IOException e) {
            log.error("Failed to initialize users file", e);
            throw new RuntimeException("Failed to initialize user storage", e);
        }
    }

    /**
     * Create three default admin users at startup
     */
    private void createDefaultAdminUsers() {
        String[][] admins = {
            {"admin", "System Administrator", "admin123"},
            {"superadmin", "Super Administrator", "super123"},
            {"root", "Root Administrator", "root123"}
        };

        try {
            List<User> users = loadAllUsers();
            boolean needsSave = false;

            for (String[] admin : admins) {
                if (users.stream().noneMatch(u -> u.getShortName().equals(admin[0]))) {
                    User adminUser = new User(admin[0], admin[1], passwordEncoder.encode(admin[2]));
                    adminUser.setRole(User.Role.ADMIN);
                    users.add(adminUser);
                    needsSave = true;
                    log.info("Created default admin user: {}", admin[0]);
                }
            }

            if (needsSave) {
                saveAllUsers(users);
            }

        } catch (Exception e) {
            log.error("Failed to create admin users", e);
        }
    }

    /**
     * Load all users from JSON file
     */
    private List<User> loadAllUsers() throws IOException {
        if (!Files.exists(usersFilePath)) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(usersFilePath.toFile(), new TypeReference<List<User>>() {});
        } catch (IOException e) {
            log.error("Failed to read users file", e);
            return new ArrayList<>();
        }
    }

    /**
     * Save all users to JSON file
     */
    private void saveAllUsers(List<User> users) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(usersFilePath.toFile(), users);
        log.debug("Saved {} users to file", users.size());
    }

    /**
     * Find user by short name
     */
    public Optional<User> findByShortName(String shortName) {
        try {
            List<User> users = loadAllUsers();
            return users.stream()
                    .filter(user -> user.getShortName().equals(shortName))
                    .findFirst();
        } catch (IOException e) {
            log.error("Failed to load users for search: {}", shortName, e);
            return Optional.empty();
        }
    }

    /**
     * Register new user with encrypted password
     */
    public User registerUser(String shortName, String fullName, String password) throws IOException {
        if (userExists(shortName)) {
            throw new IllegalArgumentException("User already exists: " + shortName);
        }

        List<User> users = loadAllUsers();
        User user = new User(shortName, fullName, passwordEncoder.encode(password));
        user.setRole(User.Role.USER); // Regular users get USER role

        users.add(user);
        saveAllUsers(users);

        log.info("Registered new user: {}", shortName);
        return user;
    }

    /**
     * Authenticate user credentials
     */
    public Optional<User> authenticateUser(String shortName, String password) {
        Optional<User> userOpt = findByShortName(shortName);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                log.info("User authenticated successfully: {}", shortName);
                return Optional.of(user);
            } else {
                log.warn("Authentication failed for user: {} - invalid password", shortName);
            }
        } else {
            log.warn("Authentication failed for user: {} - user not found", shortName);
        }

        return Optional.empty();
    }

    /**
     * Check if user exists
     */
    public boolean userExists(String shortName) {
        return findByShortName(shortName).isPresent();
    }
}
