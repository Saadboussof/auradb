package com.auradb.common.model;

import java.time.Instant;

/**
 * Metrics emitted by a database node.
 */
public record TelemetryEvent(
        String instanceId,
        String tenantId,
        double cpuPercent,
        double memoryPercent,
        int activeConnections,
        double queryLatencyMs,
        double diskUsagePercent,
        Instant timestamp
) {}
