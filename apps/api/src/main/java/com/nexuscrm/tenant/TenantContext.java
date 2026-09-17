package com.nexuscrm.tenant;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId, UUID userId, String role) {
        TENANT.set(tenantId);
        USER.set(userId);
        ROLE.set(role);
    }

    public static UUID tenantId() {
        return TENANT.get();
    }

    public static UUID requireTenantId() {
        UUID id = TENANT.get();
        if (id == null) {
            throw new IllegalStateException("Tenant context is not set");
        }
        return id;
    }

    public static UUID userId() {
        return USER.get();
    }

    public static String role() {
        return ROLE.get();
    }

    public static void clear() {
        TENANT.remove();
        USER.remove();
        ROLE.remove();
    }
}
