package com.pm.authservice.service;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Authentication Service
 * <p>
 * This service handles user authentication operations in the authentication microservice.
 * It works in conjunction with the UserService to validate credentials and the JwtUtil
 * to generate and validate authentication tokens.
 * <p>
 * In the API Gateway architecture, this service is responsible for:
 * - Authenticating users by validating their credentials
 * - Generating JWT tokens for authenticated users
 * - Validating JWT tokens when requested
 * <p>
 * The authentication flow typically begins at the API Gateway, which forwards
 * authentication requests to this service. Upon successful authentication,
 * the generated token can be used for subsequent requests through the gateway.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Constructs an AuthService with required dependencies.
     *
     * @param userService     The service that provides user data access operations
     * @param passwordEncoder The encoder used to verify password matches
     * @param jwtUtil         The utility that handles JWT token generation and validation
     */
    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates a user based on email and password credentials.
     * <p>
     * This method:
     * 1. Attempts to find a user with the provided email
     * 2. If found, verifies the provided password against the stored hash
     * 3. If verified, generates a JWT token containing the user's email and role
     * <p>
     * The authentication process uses Spring Security's password encoder to securely
     * compare passwords without exposing the actual password values.
     *
     * @param loginRequestDTO Data transfer object containing login credentials (email and password)
     * @return An Optional containing the JWT token if authentication succeeds, or empty if it fails
     */
    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<String> token = userService.findByEmail(loginRequestDTO.getEmail())
                .filter(u -> passwordEncoder.matches(loginRequestDTO.getPassword(), u.getPassword()))
                .map(u -> jwtUtil.generateToken(u.getEmail(), u.getRole()));

        return token;
    }

    /**
     * Validates a JWT token to ensure it is properly signed and not expired.
     * <p>
     * This method delegates to the JwtUtil to perform the actual validation.
     * It catches any JWT exceptions that might occur during validation and
     * returns a boolean result indicating whether the token is valid.
     * <p>
     * In an API Gateway architecture, token validation might be performed by:
     * - This service when explicitly requested
     * - The API Gateway itself for most requests
     * - Other services that need to verify authentication
     *
     * @param token The JWT token string to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}