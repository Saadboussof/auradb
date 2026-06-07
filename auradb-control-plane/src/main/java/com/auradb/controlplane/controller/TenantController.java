package com.auradb.controlplane.controller;

import com.auradb.common.model.Tenant;
import com.auradb.controlplane.service.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    // DTO for creating a tenant
    public record CreateTenantRequest(String id, String name, String plan) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Tenant> createTenant(@RequestBody CreateTenantRequest request) {
        return tenantService.createTenant(request.id(), request.name(), request.plan());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Tenant>> getTenant(@PathVariable String id) {
        return tenantService.getTenant(id)
                .map(ResponseEntity::ok)
                // Let the global exception handler deal with it, or handle it here:
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping
    public Flux<Tenant> getAllTenants() {
        return tenantService.getAllTenants();
    }
}
