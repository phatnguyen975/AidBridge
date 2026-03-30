package com.drc.aidbridge.config;

import com.drc.aidbridge.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for RSA-signed JWT authentication.
 *
 * Features:
 * - Stateless session management (JWT-based)
 * - RSA-signed token validation via JwtAuthenticationFilter
 * - Role-based endpoint authorization (VICTIM, VOLUNTEER, SPONSOR, STAFF,
 * ADMIN)
 * - BCrypt password encoding
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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/ws/**").permitAll() // WebSocket handshake
                        .requestMatchers("/api/sos").permitAll()
                        // Role-based endpoint authorization
                        // ADMIN - full system access
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // STAFF - hub management, can do most admin tasks
                        .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN")

                        // VOLUNTEER - mission handling
                        .requestMatchers("/api/volunteer/**").hasAnyRole("VOLUNTEER", "STAFF", "ADMIN")

                        // SPONSOR - donation management
                        .requestMatchers("/api/sponsor/**").hasAnyRole("SPONSOR", "STAFF", "ADMIN")

                        // VICTIM - aid requests, accessible by all authenticated users
                        .requestMatchers("/api/victim/**")
                        .hasAnyRole("VICTIM", "VOLUNTEER", "SPONSOR", "STAFF", "ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated())
                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder with strength 12.
     * Higher strength = more secure but slower.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
