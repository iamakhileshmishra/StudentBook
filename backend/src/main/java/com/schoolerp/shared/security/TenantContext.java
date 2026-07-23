package com.schoolerp.shared.security;

import java.util.UUID;

/**
 * Thread-local holder for the current school (tenant) ID.
 * Set by JwtAuthFilter from the JWT token on every request.
 * All repository queries must filter by this value.
 */
public class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_SCHOOL_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static void setSchoolId(UUID schoolId) {
        CURRENT_SCHOOL_ID.set(schoolId);
    }

    public static UUID getSchoolId() {
        return CURRENT_SCHOOL_ID.get();
    }

    public static void clear() {
        CURRENT_SCHOOL_ID.remove();
    }
}
