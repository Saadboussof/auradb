package com.auradb.common.model;

import java.time.Instant;

/**
 * Represents the current routing state for a tenant.
 */
public record RouteInfo(
        String tenantId,
        String activeEndpoint,
        String fallbackEndpoint,
        Instant updatedAt
) {}
