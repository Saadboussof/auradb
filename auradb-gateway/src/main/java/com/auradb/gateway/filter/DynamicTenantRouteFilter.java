package com.auradb.gateway.filter;

import com.auradb.common.model.RouteInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DynamicTenantRouteFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DynamicTenantRouteFilter.class);
    
    // We intercept calls like: /api/query/netflix/users
    // Group 1 = netflix
    // Group 2 = /users
    private static final Pattern QUERY_PATTERN = Pattern.compile("^/api/query/([^/]+)(/.*)?$");
    
    private final ReactiveRedisTemplate<String, RouteInfo> redisTemplate;

    public DynamicTenantRouteFilter(ReactiveRedisTemplate<String, RouteInfo> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        Matcher matcher = QUERY_PATTERN.matcher(path);
        
        if (matcher.matches()) {
            String tenantId = matcher.group(1);
            String remainingPath = matcher.group(2) == null ? "" : matcher.group(2);
            
            log.info("GATEWAY INTERCEPT: Received query for tenant '{}'", tenantId);
            
            // Look up the physical IP in Redis!
            return redisTemplate.opsForValue().get("route:" + tenantId)
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("GATEWAY ERROR: No IP found in Redis for tenant '{}'. Dropping request.", tenantId);
                        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                        return Mono.empty(); // Stop the request completely
                    }))
                    .flatMap(routeInfo -> {
                        try {
                            String physicalEndpoint = routeInfo.activeEndpoint() + remainingPath;
                            log.info("GATEWAY ROUTING: Teleporting query to physical IP: {}", physicalEndpoint);
                            
                            // Construct the new target URI
                            URI targetUri = new URI(physicalEndpoint);
                            
                            // Mutate the request
                            ServerWebExchange mutatedExchange = exchange.mutate()
                                    .request(exchange.getRequest().mutate().uri(targetUri).build())
                                    .build();
                                    
                            // Tell Spring Cloud Gateway exactly where to forward it
                            mutatedExchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, targetUri);
                            
                            // Continue down the chain
                            return chain.filter(mutatedExchange);
                        } catch (Exception e) {
                            log.error("GATEWAY ERROR: Failed to parse physical IP", e);
                            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                            return Mono.empty();
                        }
                    });
        }
        
        // If it's not a /api/query request, just let it pass through to standard YAML routes (like Phase 1 & 2)
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run very early in the filter chain before the actual routing happens
        return -100;
    }
}
