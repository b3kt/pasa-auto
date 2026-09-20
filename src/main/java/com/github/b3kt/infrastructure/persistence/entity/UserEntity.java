package com.github.b3kt.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.b3kt.domain.model.ApprovalStatus;
import com.github.b3kt.domain.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity for User.
 * This is the persistence representation of the domain User entity.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username", unique = true),
        @Index(name = "idx_email", columnList = "email"),
        // V20 creates these; there it is a partial unique index on google_sub and a functional
        // index on lower(email), neither of which JPA can express
        @Index(name = "idx_users_google_sub", columnList = "google_sub", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    /** Never serialized: API clients set passwords through {@link #password}. */
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Write-only plaintext from API requests; hashed into {@link #passwordHash} by the service, never stored. */
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** Set for temporary passwords (issued by an admin or migrated); the user must choose a new one before doing anything else. */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    @Column(name = "karyawan_id")
    private Long karyawanId;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * The Google account this user signs in with, bound on the first successful Google sign-in.
     * Read-only over the API: it is established by the callback, never by an admin editing a user.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "google_sub", length = 255)
    private String googleSub;

    /** Set by an Owner to let a matching verified Google email claim this account. */
    @Column(name = "google_login_enabled", nullable = false)
    private boolean googleLoginEnabled = false;

    /**
     * Read-only over the API so the generic CRUD path cannot approve an account: only the
     * dedicated approve/reject endpoints may change it.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    public UserEntity(String username, String email, String passwordHash, Set<RoleEntity> roles) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.active = true;
    }

    /**
     * Convert to domain User entity.
     */
    public User toDomain() {
        User user = new User();
        user.setUsername(this.username);
        user.setEmail(this.email);
        user.setPasswordHash(this.passwordHash);
        user.setRoles(this.roles);
        user.setActive(this.active);
        user.setMustChangePassword(this.mustChangePassword);
        user.setApprovalStatus(this.approvalStatus);
        return user;
    }

    /**
     * Create from domain User entity.
     */
    public static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRoles(user.getRoles());
        entity.setActive(user.isActive());
        entity.setMustChangePassword(user.isMustChangePassword());
        entity.setApprovalStatus(user.getApprovalStatus());
        return entity;
    }
}
