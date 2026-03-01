package com.github.b3kt.infrastructure.persistence.entity;

import com.github.b3kt.domain.model.Role;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity for Role.
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;


    public RoleEntity(String name, String description) {
        this.name = name;
        this.description = description;
        this.active = true;
    }

    /**
     * Convert to domain Role entity.
     */
    public Role toDomain() {
        Role role = new Role();
        role.setId(this.id);
        role.setName(this.name);
        role.setDescription(this.description);
        role.setActive(this.active);
        role.setPermissions(this.permissions.stream()
                .map(PermissionEntity::toDomain)
                .collect(Collectors.toSet()));
        return role;
    }

    /**
     * Create from domain Role entity.
     */
    public static RoleEntity fromDomain(Role role) {
        RoleEntity entity = new RoleEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setActive(role.isActive());
        if (role.getPermissions() != null) {
            entity.setPermissions(role.getPermissions().stream()
                    .map(PermissionEntity::fromDomain)
                    .collect(Collectors.toSet()));
        }
        return entity;
    }

    // Custom setter for permissions with null handling
    public void setPermissions(Set<PermissionEntity> permissions) {
        this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
    }
}

