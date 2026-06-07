package com.auradb.common.model;

import java.time.Instant;

public record DbMetric(String instanceId, double cpuUsage, double memoryUsage, long latencyMs, Instant timestamp) {
    public static DbMetric createHealthy(String instanceId) {
        return new DbMetric(instanceId, 30.0, 45.0, 15, Instant.now());
    }

    public static DbMetric createCrashed(String instanceId) {
        return new DbMetric(instanceId, 99.9, 98.5, 5000, Instant.now());
    }
}
