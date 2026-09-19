package com.github.b3kt.infrastructure.security;

/**
 * Role names as stored in the {@code roles} table and issued in the JWT {@code groups} claim.
 * Role checks are case-sensitive, so endpoints must reference these constants instead of string literals.
 */
public final class Roles {

    public static final String ADMIN = "Admin";
    public static final String OWNER = "Owner";
    public static final String KARYAWAN = "Karyawan";

    private Roles() {
    }
}
