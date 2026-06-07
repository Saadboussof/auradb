package com.auradb.common.model;

import java.time.Instant;

/**
 * Represents a customer (tenant) of AuraDB.
 *
 * Example:
 *   id       = "netflix"
 *   name     = "Netflix Inc."
 *   plan     = "enterprise"
 */
public record Tenant(
        String id,
        String name,
        String plan,          // "starter", "professional", "enterprise"
        String status,        // "ACTIVE", "SUSPENDED", "DEPROVISIONED"
        Instant createdAt
) {
    /**
     * Factory method to create a new active tenant.
     */
    public static Tenant create(String id, String name, String plan) {
        return new Tenant(id, name, plan, "ACTIVE", Instant.now());
    }
}
