package com.pm.authservice.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

/**
 * JWT (JSON Web Token) Utility Class
 * <p>
 * This class provides functionality for generating and validating JSON Web Tokens (JWTs).
 * JWTs are used for securely transmitting information between parties as a compact,
 * self-contained token that can be verified and trusted because it is digitally signed.
 * <p>
 * In an API Gateway architecture, this service generates tokens that can be validated
 * by the gateway or other services to authenticate and authorize requests.
 */
@Component
public class JwtUtil {

    /**
     * The secret cryptographic key used to sign JWT tokens.
     * This key is crucial for the security of the tokens - anyone with access to this key
     * can generate valid tokens or validate existing ones.
     */
    private final Key secretKey;

    /**
     * Constructor that initializes the signing key from a Base64-encoded secret.
     *
     * @param secret The JWT secret key injected from application properties.
     *               This should be a strong, Base64-encoded secret value.
     */
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // Decode the Base64-encoded secret into raw bytes
        byte[] keyBytes = Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8));
        // Create an HMAC-SHA signing key from the decoded bytes
        // This creates a cryptographically secure key for JWT signing
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a new JWT token containing user information.
     * <p>
     * The generated token includes:
     * - Subject (user email): Identifies the principal user
     * - Role claim: Specifies the user's authorization level
     * - Issued time: When the token was created
     * - Expiration time: When the token becomes invalid (10 hours from creation)
     * - Digital signature: Ensures the token hasn't been tampered with
     *
     * @param email The user's email address to be stored as the subject
     * @param role  The user's role for authorization purposes
     * @return A signed JWT token as a compact string
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                // Set the token subject (typically user identifier)
                .subject(email)
                // Add custom claims - in this case, the user's role
                .claim("role", role)
                // Record when the token was issued
                .issuedAt(new Date())
                // Set token expiration (10 hours from now)
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                // Sign the token with our secret key
                .signWith(secretKey)
                // Build the final token as a compact, URL-safe string
                .compact();
    }

    /**
     * Validates a JWT token to ensure it is properly signed and not expired.
     * <p>
     * This method attempts to parse and verify the token using the same secret key
     * that was used to sign it. If the token is invalid (wrong signature, expired,
     * malformed, etc.), a JwtException is thrown.
     *
     * @param token The JWT token string to validate
     * @throws JwtException If the token is invalid for any reason
     */
    public void validateToken(String token) {
        try {
            // Create a parser configured with our signing key
            Jwts.parser().verifyWith((SecretKey) secretKey).build()
                    // Parse and verify the token
                    .parseSignedClaims(token);
        } catch (SignatureException e) {
            // Thrown if the token's signature doesn't match our key
            // This indicates the token was tampered with or signed by a different key
            throw new JwtException("Invalid JWT signature");
        } catch (JwtException e) {
            // Catches other JWT-related exceptions (expired, malformed, etc.)
            throw new JwtException("Invalid JWT");
        }
    }
}