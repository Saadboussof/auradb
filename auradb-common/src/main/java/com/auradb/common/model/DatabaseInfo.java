package com.auradb.common.model;

import java.time.Instant;

/**
 * Represents a specific physical or logical database instance allocated to a tenant.
 */
public record DatabaseInfo(
        String instanceId,
        String tenantId,
        String engine,        // "postgresql", "mysql"
        String endpoint,      // e.g. "http://10.0.0.1:5432"
        String status,        // "PROVISIONING", "HEALTHY", "DEGRADED", "DOWN"
        String role,          // "PRIMARY", "REPLICA"
        Instant createdAt
) {}
