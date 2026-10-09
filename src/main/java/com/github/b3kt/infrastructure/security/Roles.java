package com.github.b3kt.infrastructure.security;

/**
 * Role names as stored in the {@code roles} table and issued in the JWT {@code groups} claim.
 * Role checks are case-sensitive, so endpoints must reference these constants instead of string literals.
 */
public final class Roles {

    public static final String ADMIN = "Admin";
    public static final String OWNER = "Owner";
    public static final String KARYAWAN = "Karyawan";

    /**
     * Issued only in customer-portal JWTs (see {@code CustomerJwtTokenServiceImpl}), never assigned
     * via the staff {@code roles} table. Staff tokens never carry this group, and customer tokens
     * never carry Admin/Owner/Karyawan, so the two identity spaces stay mutually unreachable.
     */
    public static final String CUSTOMER = "Customer";

    private Roles() {
    }
}
