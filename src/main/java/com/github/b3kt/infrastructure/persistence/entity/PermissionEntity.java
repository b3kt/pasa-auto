package com.github.b3kt.infrastructure.persistence.entity;

import com.github.b3kt.domain.model.Permission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity for Permission.
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_name", columnList = "name", unique = true),
    @Index(name = "idx_permission_resource_action", columnList = "resource,action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 50)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false)
    private boolean active = true;


    public PermissionEntity(String name, String description, String resource, String action) {
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.action = action;
    }

    /**
     * Convert to domain Permission entity.
     */
    public Permission toDomain() {
        Permission permission = new Permission();
        permission.setId(this.id);
        permission.setName(this.name);
        permission.setDescription(this.description);
        permission.setResource(this.resource);
        permission.setAction(this.action);
        permission.setActive(this.active);
        return permission;
    }

    /**
     * Create from domain Permission entity.
     */
    public static PermissionEntity fromDomain(Permission permission) {
        PermissionEntity entity = new PermissionEntity();
        entity.setId(permission.getId());
        entity.setName(permission.getName());
        entity.setDescription(permission.getDescription());
        entity.setResource(permission.getResource());
        entity.setAction(permission.getAction());
        entity.setActive(permission.isActive());
        return entity;
    }
}

