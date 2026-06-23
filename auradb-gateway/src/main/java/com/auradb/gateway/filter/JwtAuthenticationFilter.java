package com.auradb.gateway.filter;

import com.auradb.gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // Skip auth for internal admin endpoints and token generation
        if (path.startsWith("/api/auth/") || path.startsWith("/api/v1/tenants") || path.startsWith("/api/v1/routes")) {
            return chain.filter(exchange);
        }

        // Only secure the query endpoints for this phase
        if (path.startsWith("/api/query/")) {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("SECURITY BLOCKED: Missing or invalid Authorization header for path {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.validateToken(token);
                String tenantIdFromToken = claims.getSubject();
                log.info("SECURITY PASSED: Valid token provided for tenant '{}'", tenantIdFromToken);
                
                // Add the tenantId as a header so downstream services know who called them
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header("X-Authenticated-Tenant", tenantIdFromToken)
                                .build())
                        .build();

                return chain.filter(mutatedExchange);
            } catch (Exception e) {
                log.error("SECURITY BLOCKED: Invalid token provided. Error: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run BEFORE the DynamicTenantRouteFilter (which is -100)
        return -200; 
    }
}
