package com.auradb.common.exception;

/**
 * Custom exception for when a tenant does not exist.
 */
public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
    }
}
