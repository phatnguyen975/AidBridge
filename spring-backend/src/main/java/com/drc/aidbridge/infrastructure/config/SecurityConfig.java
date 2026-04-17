package com.drc.aidbridge.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.drc.aidbridge.infrastructure.security.JwtAuthenticationFilter;

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
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/favicon.ico", "/error").permitAll()
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/ws/**").permitAll() // WebSocket handshake
                        // Permit for springdoc OpenAPI
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sos-requests").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sos-requests").permitAll()
                        .requestMatchers("/api/sos-requests/**").permitAll() // Allow public access to SOS request endpoints
                        .requestMatchers(HttpMethod.GET, "/api/hubs", "/api/hubs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/hubs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/hubs/*/inventory/import").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/hubs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/donations").hasAnyRole("SPONSOR", "STAFF", "ADMIN")
                        .requestMatchers("/api/donations/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/api/routing/**").permitAll() // GraphHopper routing endpoints
                        // Role-based endpoint authorization
                        // ADMIN - full system access
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")



                        // STAFF - hub management, can do most admin tasks
                        .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN")

                        // VOLUNTEER - mission handling
                        .requestMatchers("/api/volunteers/**").hasAnyRole("VOLUNTEER", "STAFF", "ADMIN")

                        // SPONSOR - donation management
                        .requestMatchers("/api/sponsor/**").hasAnyRole("SPONSOR", "STAFF", "ADMIN")

                        // VICTIM - aid requests, accessible by all authenticated users
                        .requestMatchers("/api/aid-requests/**")
                        .hasAnyRole("VICTIM", "VOLUNTEER", "SPONSOR", "STAFF", "ADMIN")
                        .requestMatchers("/api/aid-requests/**")
                        .hasAnyRole("VICTIM", "VOLUNTEER", "SPONSOR", "STAFF", "ADMIN")
                        .requestMatchers("/api/victim/**")
                        .hasAnyRole("VICTIM", "VOLUNTEER", "SPONSOR", "STAFF", "ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated())
                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
