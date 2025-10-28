package com.sajid.serverManagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration following Spring Security best practices
 * Configures JWT-based authentication and role-based authorization
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF as we're using JWT tokens
            .csrf(AbstractHttpConfigurer::disable)

            // Configure session management to be stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/**").permitAll()

                // File upload and extraction endpoints - ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/files/upload").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/files/upload-and-extract").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/files/extract").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/files/extract-async").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/files/extraction-status/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/files/extraction-status/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/files/delete").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/files/info").hasAnyRole("USER", "ADMIN")

                // GET endpoints - all authenticated users can access
                .requestMatchers(HttpMethod.GET, "/servers/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/servers").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/debug/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/debug").hasAnyRole("USER", "ADMIN")

                // POST endpoints - only ADMIN can access (server management operations)
                .requestMatchers(HttpMethod.POST, "/servers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/servers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/servers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/start/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/stop/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
