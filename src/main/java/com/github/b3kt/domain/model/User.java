package com.github.b3kt.domain.model;

import java.util.Set;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * Domain entity representing a User.
 * This is the core domain model with business logic.
 */
@Getter
@Setter
public class User {
    private String username;
    private String email;
    private String passwordHash;
    private Set<RoleEntity> roles;
    private boolean active;

    private transient Long karyawanId;
    private transient String karyawanNama;

    public User() {
    }

    public User(String username, String email, String passwordHash, Set<RoleEntity> roles) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.active = true;
    }

    public User(String username, String email, String passwordHash, Set<RoleEntity> roles, Long karyawanId,
            String karyawanNama) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.active = true;
        this.karyawanId = karyawanId;
        this.karyawanNama = karyawanNama;
    }

    /**
     * Business logic: Check if user can authenticate
     */
    public boolean canAuthenticate() {
        return active && username != null && !username.isEmpty();
    }
}
