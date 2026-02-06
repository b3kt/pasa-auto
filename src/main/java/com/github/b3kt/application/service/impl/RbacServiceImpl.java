package com.github.b3kt.application.service.impl;

import com.github.b3kt.application.properties.RbacProperties;
import com.github.b3kt.application.service.RbacService;
import com.github.b3kt.domain.exception.UserNotFoundException;
import com.github.b3kt.domain.model.Permission;
import com.github.b3kt.domain.model.Role;
import com.github.b3kt.infrastructure.persistence.entity.PermissionEntity;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.PermissionEntityRepository;
import com.github.b3kt.infrastructure.persistence.repository.RoleEntityRepository;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of RBAC service.
 */
@ApplicationScoped
public class RbacServiceImpl implements RbacService {

    @Inject
    RbacProperties rbacProperties;

    @Inject
    RoleEntityRepository roleEntityRepository;

    @Inject
    PermissionEntityRepository permissionEntityRepository;

    @Inject
    UserEntityRepository userEntityRepository;

    private void checkRbacEnabled() {
        if (!rbacProperties.enabled()) {
            throw new IllegalStateException(
                    "RBAC module is not enabled. Set app.rbac.enabled=true to use RBAC features.");
        }
    }

    // Role operations
    @Override
    @Transactional
    public Role createRole(String name, String description) {
        checkRbacEnabled();
        if (roleEntityRepository.existsByName(name)) {
            throw new IllegalArgumentException("Role with name '" + name + "' already exists");
        }
        RoleEntity entity = new RoleEntity(name, description);
        roleEntityRepository.persist(entity);
        return entity.toDomain();
    }

    @Override
    @Transactional
    public Role updateRole(Long roleId, String name, String description) {
        checkRbacEnabled();
        RoleEntity entity = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        if (name != null && !name.equals(entity.getName())) {
            if (roleEntityRepository.existsByName(name)) {
                throw new IllegalArgumentException("Role with name '" + name + "' already exists");
            }
            entity.setName(name);
        }
        if (description != null) {
            entity.setDescription(description);
        }
        roleEntityRepository.persist(entity);
        return entity.toDomain();
    }

    @Override
    public Role getRoleById(Long roleId) {
        checkRbacEnabled();
        return roleEntityRepository.findByIdOptional(roleId)
                .map(RoleEntity::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
    }

    @Override
    public Role getRoleByName(String name) {
        checkRbacEnabled();
        return roleEntityRepository.findByName(name)
                .map(RoleEntity::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with name: " + name));
    }

    @Override
    public List<Role> getAllRoles() {
        checkRbacEnabled();
        return roleEntityRepository.listAll().stream()
                .map(RoleEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Role> getActiveRoles() {
        checkRbacEnabled();
        return roleEntityRepository.findActiveRoles().stream()
                .map(RoleEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        checkRbacEnabled();
        RoleEntity entity = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        roleEntityRepository.delete(entity);
    }

    @Override
    @Transactional
    public void activateRole(Long roleId) {
        checkRbacEnabled();
        RoleEntity entity = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        entity.setActive(true);
        roleEntityRepository.persist(entity);
    }

    @Override
    @Transactional
    public void deactivateRole(Long roleId) {
        checkRbacEnabled();
        RoleEntity entity = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        entity.setActive(false);
        roleEntityRepository.persist(entity);
    }

    // Permission operations
    @Override
    @Transactional
    public Permission createPermission(String name, String description, String resource, String action) {
        checkRbacEnabled();
        if (permissionEntityRepository.existsByName(name)) {
            throw new IllegalArgumentException("Permission with name '" + name + "' already exists");
        }
        PermissionEntity entity = new PermissionEntity(name, description, resource, action);
        permissionEntityRepository.persist(entity);
        return entity.toDomain();
    }

    @Override
    @Transactional
    public Permission updatePermission(Long permissionId, String name, String description, String resource,
            String action) {
        checkRbacEnabled();
        PermissionEntity entity = permissionEntityRepository.findByIdOptional(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + permissionId));
        if (name != null && !name.equals(entity.getName())) {
            if (permissionEntityRepository.existsByName(name)) {
                throw new IllegalArgumentException("Permission with name '" + name + "' already exists");
            }
            entity.setName(name);
        }
        if (description != null) {
            entity.setDescription(description);
        }
        if (resource != null) {
            entity.setResource(resource);
        }
        if (action != null) {
            entity.setAction(action);
        }
        permissionEntityRepository.persist(entity);
        return entity.toDomain();
    }

    @Override
    public Permission getPermissionById(Long permissionId) {
        checkRbacEnabled();
        return permissionEntityRepository.findByIdOptional(permissionId)
                .map(PermissionEntity::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + permissionId));
    }

    @Override
    public Permission getPermissionByName(String name) {
        checkRbacEnabled();
        return permissionEntityRepository.findByName(name)
                .map(PermissionEntity::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with name: " + name));
    }

    @Override
    public List<Permission> getAllPermissions() {
        checkRbacEnabled();
        return permissionEntityRepository.listAll().stream()
                .map(PermissionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Permission> getActivePermissions() {
        checkRbacEnabled();
        return permissionEntityRepository.findActivePermissions().stream()
                .map(PermissionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Permission> getPermissionsByResource(String resource) {
        checkRbacEnabled();
        return permissionEntityRepository.findByResource(resource).stream()
                .map(PermissionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePermission(Long permissionId) {
        checkRbacEnabled();
        PermissionEntity entity = permissionEntityRepository.findByIdOptional(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + permissionId));
        permissionEntityRepository.delete(entity);
    }

    @Override
    @Transactional
    public void activatePermission(Long permissionId) {
        checkRbacEnabled();
        PermissionEntity entity = permissionEntityRepository.findByIdOptional(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + permissionId));
        entity.setActive(true);
        permissionEntityRepository.persist(entity);
    }

    @Override
    @Transactional
    public void deactivatePermission(Long permissionId) {
        checkRbacEnabled();
        PermissionEntity entity = permissionEntityRepository.findByIdOptional(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + permissionId));
        entity.setActive(false);
        permissionEntityRepository.persist(entity);
    }

    // Role-Permission operations
    @Override
    @Transactional
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        checkRbacEnabled();
        RoleEntity role = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        PermissionEntity permission = permissionEntityRepository.findByIdOptional(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + permissionId));
        role.getPermissions().add(permission);
        roleEntityRepository.persist(role);
    }

    @Override
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        checkRbacEnabled();
        RoleEntity role = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        PermissionEntity permission = permissionEntityRepository.findByIdOptional(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + permissionId));
        role.getPermissions().remove(permission);
        roleEntityRepository.persist(role);
    }

    @Override
    public Set<Permission> getRolePermissions(Long roleId) {
        checkRbacEnabled();
        RoleEntity role = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        return role.getPermissions().stream()
                .map(PermissionEntity::toDomain)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean roleHasPermission(Long roleId, String permissionName) {
        checkRbacEnabled();
        RoleEntity role = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        return role.getPermissions().stream()
                .anyMatch(p -> p.getName().equals(permissionName) && p.isActive());
    }

    // User-Role operations
    @Override
    @Transactional
    public void assignRoleToUser(String username, Long roleId) {
        checkRbacEnabled();
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        RoleEntity role = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        user.getRoles().add(role);
        userEntityRepository.persist(user);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(String username, Long roleId) {
        checkRbacEnabled();
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        RoleEntity role = roleEntityRepository.findByIdOptional(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        user.getRoles().remove(role);
        userEntityRepository.persist(user);
    }

    @Override
    public Set<Role> getUserRoles(String username) {
        checkRbacEnabled();
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        return user.getRoles().stream()
                .map(RoleEntity::toDomain)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Permission> getUserPermissions(String username) {
        checkRbacEnabled();
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        Set<Permission> permissions = new HashSet<>();
        for (RoleEntity role : user.getRoles()) {
            if (role.isActive()) {
                for (PermissionEntity permission : role.getPermissions()) {
                    if (permission.isActive()) {
                        permissions.add(permission.toDomain());
                    }
                }
            }
        }
        return permissions;
    }

    @Override
    public boolean userHasPermission(String username, String permissionName) {
        checkRbacEnabled();
        Set<Permission> userPermissions = getUserPermissions(username);
        return userPermissions.stream()
                .anyMatch(p -> p.getName().equals(permissionName));
    }

    @Override
    public boolean userHasRole(String username, String roleName) {
        checkRbacEnabled();
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().equals(roleName) && r.isActive());
    }
}
