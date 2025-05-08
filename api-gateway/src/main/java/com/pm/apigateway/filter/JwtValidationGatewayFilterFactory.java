package com.pm.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * JWT Validation Gateway Filter Factory
 * <p>
 * This class implements a Spring Cloud Gateway filter factory that validates JWT tokens
 * in incoming requests before allowing them to proceed to backend services.
 *
 * <h2>Naming Convention</h2>
 * The class name follows Spring Cloud Gateway's naming convention for filter factories:
 * <ul>
 *   <li>The name must end with "GatewayFilterFactory"</li>
 *   <li>The prefix before "GatewayFilterFactory" determines how the filter is referenced in configuration</li>
 * </ul>
 * <p>
 * For this class (JwtValidationGatewayFilterFactory), the filter can be referenced in
 * application.yml as "JwtValidation" (without the "GatewayFilterFactory" suffix).
 *
 * <h2>Configuration Example</h2>
 * In application.yml:
 * <pre>
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: secured-service-route
 *           uri: http://some-service:8080
 *           predicates:
 *             - Path=/api/**
 *           filters:
 *             - JwtValidation
 * </pre>
 *
 * <h2>Authentication Flow</h2>
 * This filter:
 * 1. Extracts the JWT token from the Authorization header
 * 2. Validates the token by making a request to the auth service
 * 3. If valid, allows the request to proceed to the backend service
 * 4. If invalid, returns a 401 Unauthorized response
 * <p>
 * This implementation delegates token validation to the auth-service rather than
 * performing validation itself, which centralizes authentication logic.
 */
@Component
public class JwtValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final WebClient webClient;

    /**
     * Constructs a new JWT validation filter factory.
     *
     * @param webClientBuilder The WebClient.Builder to create HTTP clients
     * @param authServiceUrl   The URL of the authentication service that will validate tokens
     */
    public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder, @Value("${auth.service.url}") String authServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }

    /**
     * Creates a GatewayFilter instance that performs JWT validation.
     * <p>
     * This method is called by Spring Cloud Gateway when processing a request
     * that matches a route with the "JwtValidation" filter configured.
     *
     * @param config The filter configuration (not used in this implementation)
     * @return A GatewayFilter that validates JWT tokens
     */
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            // Extract the Authorization header from the request
            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Check if the token exists and has the correct format
            if (token == null || !token.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Forward the token to the auth service for validation
            return webClient.get().uri("/validate").header(HttpHeaders.AUTHORIZATION, token).retrieve().toBodilessEntity().then(chain.filter(exchange));
        };
    }
}