package com.github.b3kt.infrastructure.persistence.entity;

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
        @Index(name = "idx_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    @Column(name = "karyawan_id")
    private Long karyawanId;

    @Column(nullable = false)
    private boolean active = true;

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
        return entity;
    }
}
