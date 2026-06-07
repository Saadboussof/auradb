package com.auradb.controlplane.service;

import com.auradb.common.exception.TenantNotFoundException;
import com.auradb.common.model.Tenant;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TenantService {

    // Using an in-memory map for Phase 1. (Phase 7 will move this to PostgreSQL)
    private final Map<String, Tenant> tenantStore = new ConcurrentHashMap<>();

    public Mono<Tenant> createTenant(String id, String name, String plan) {
        if (tenantStore.containsKey(id)) {
            return Mono.error(new IllegalArgumentException("Tenant ID already exists: " + id));
        }
        
        Tenant newTenant = Tenant.create(id, name, plan);
        tenantStore.put(id, newTenant);
        
        return Mono.just(newTenant);
    }

    public Mono<Tenant> getTenant(String id) {
        return Mono.justOrEmpty(tenantStore.get(id))
                .switchIfEmpty(Mono.error(new TenantNotFoundException(id)));
    }

    public Flux<Tenant> getAllTenants() {
        return Flux.fromIterable(tenantStore.values());
    }
}
