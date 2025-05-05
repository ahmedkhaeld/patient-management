package com.pm.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration for Auth Service in API Gateway Architecture
 * <p>
 * This class configures security settings for the authentication service that operates
 * behind an API Gateway. In this architecture, the API Gateway serves as the primary
 * entry point for client requests, and this service handles authentication operations.
 * <p>
 * The @Configuration annotation marks this class as a source of bean definitions
 * for the application context.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures the security filter chain for requests coming from the API Gateway.
     * <p>
     * In an API Gateway architecture, this service typically:
     * - Processes authentication requests (login, token generation)
     * - Validates credentials and issues tokens
     * - Handles user registration and management
     * <p>
     * The API Gateway, not this service, typically handles:
     * - Token validation for most service requests
     * - Routing authenticated requests to appropriate services
     * - Perimeter security concerns
     *
     * @param http The HttpSecurity object used to configure web-based security
     * @return A fully configured SecurityFilterChain
     * @throws Exception If an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configure authorization rules for HTTP requests
                .authorizeHttpRequests(authorize ->
                        // This setting allows all requests to be permitted without authentication
                        // This is appropriate when the API Gateway handles the initial request filtering
                        // and this service is only accessible through the internal network
                        authorize.anyRequest().permitAll())
                // Disable CSRF (Cross-Site Request Forgery) protection
                // CSRF protection is typically unnecessary for API services behind a gateway
                // since requests aren't coming directly from browsers with cookies
                .csrf(AbstractHttpConfigurer::disable);

        // Build and return the configured security filter chain
        return http.build();
    }

    /**
     * Creates a password encoder bean for securely hashing user credentials.
     * <p>
     * Even in an API Gateway architecture, secure password storage remains critical.
     * This service is responsible for validating credentials during authentication,
     * and BCrypt provides strong protection for stored passwords.
     * <p>
     * BCryptPasswordEncoder features:
     * - Automatic salt generation integrated into the hash
     * - Adaptive work factor that can be increased as computing power grows
     * - Protection against rainbow table and brute force attacks
     * - Industry-standard approach for password security
     *
     * @return A BCryptPasswordEncoder instance for password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}