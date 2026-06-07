package com.auradb.route.service;

import com.auradb.common.model.RouteInfo;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class RouteService {

    // The key prefix in Redis. E.g., "route:netflix"
    private static final String KEY_PREFIX = "route:";
    
    private final ReactiveRedisTemplate<String, RouteInfo> redisTemplate;

    public RouteService(ReactiveRedisTemplate<String, RouteInfo> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<RouteInfo> saveRoute(String tenantId, String activeEndpoint, String fallbackEndpoint) {
        RouteInfo routeInfo = new RouteInfo(tenantId, activeEndpoint, fallbackEndpoint, Instant.now());
        
        // Save it to Redis, and when done, return the saved object
        return redisTemplate.opsForValue()
                .set(KEY_PREFIX + tenantId, routeInfo)
                .thenReturn(routeInfo);
    }

    public Mono<RouteInfo> getRoute(String tenantId) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + tenantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Route not found for tenant: " + tenantId)));
    }
}
