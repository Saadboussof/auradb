package com.auradb.route.controller;

import com.auradb.common.model.RouteInfo;
import com.auradb.route.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    public record SaveRouteRequest(String tenantId, String activeEndpoint, String fallbackEndpoint) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RouteInfo> saveRoute(@RequestBody SaveRouteRequest request) {
        return routeService.saveRoute(request.tenantId(), request.activeEndpoint(), request.fallbackEndpoint());
    }

    @GetMapping("/{tenantId}")
    public Mono<ResponseEntity<RouteInfo>> getRoute(@PathVariable String tenantId) {
        return routeService.getRoute(tenantId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }
}
