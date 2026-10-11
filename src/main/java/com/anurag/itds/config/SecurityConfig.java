package com.anurag.itds.config;

import com.anurag.itds.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Security configuration for the stateless, JWT-based SafeWalk API.
 *
 * <ul>
 *   <li>No HTTP sessions / no CSRF (pure token auth).</li>
 *   <li>CORS is delegated to the shared {@link CorsConfigurationSource} bean from
 *       {@link CorsConfig}, driven by {@code safewalk.cors.allowed-origins}.</li>
 *   <li>Public: auth endpoints, the read-only help-location directory, and the
 *       existing {@code /api/geocode} + {@code /api/route} endpoints (kept intact).</li>
 *   <li>Everything else requires authentication. Report moderation additionally
 *       requires the {@code MODERATOR} role.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- Public endpoints ---
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/help-locations", "/api/help-locations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/safety-data/**").permitAll()
                        .requestMatchers("/api/geocode", "/api/route").permitAll()
                        // Moderation is restricted to moderators (authorized users only).
                        .requestMatchers(HttpMethod.POST, "/api/community-reports/*/moderate").hasRole("MODERATOR")
                        // --- Everything else requires a valid JWT ---
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(request, response, HttpStatus.UNAUTHORIZED,
                                        "Authentication required. Please sign in."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(request, response, HttpStatus.FORBIDDEN,
                                        "You do not have permission to perform this action."))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private void writeError(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        // Spring Boot 4 uses Jackson 3 (tools.jackson.* packages).
        new tools.jackson.databind.ObjectMapper().writeValue(response.getOutputStream(), body);
    }
}
