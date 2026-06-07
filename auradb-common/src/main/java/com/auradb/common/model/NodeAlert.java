package com.auradb.common.model;

import java.time.Instant;

public record NodeAlert(String instanceId, String status, Instant timestamp) {
    public static NodeAlert create(String instanceId, String status) {
        return new NodeAlert(instanceId, status, Instant.now());
    }
}
