package com.github.b3kt.application.service;

import com.github.b3kt.domain.model.Permission;
import com.github.b3kt.domain.model.Role;

import java.util.List;
import java.util.Set;

/**
 * Application service interface for RBAC operations.
 */
public interface RbacService {
    
    // Role operations
    Role createRole(String name, String description);
    Role updateRole(Long roleId, String name, String description);
    Role getRoleById(Long roleId);
    Role getRoleByName(String name);
    List<Role> getAllRoles();
    List<Role> getActiveRoles();
    void deleteRole(Long roleId);
    void activateRole(Long roleId);
    void deactivateRole(Long roleId);
    
    // Permission operations
    Permission createPermission(String name, String description, String resource, String action);
    Permission updatePermission(Long permissionId, String name, String description, String resource, String action);
    Permission getPermissionById(Long permissionId);
    Permission getPermissionByName(String name);
    List<Permission> getAllPermissions();
    List<Permission> getActivePermissions();
    List<Permission> getPermissionsByResource(String resource);
    void deletePermission(Long permissionId);
    void activatePermission(Long permissionId);
    void deactivatePermission(Long permissionId);
    
    // Role-Permission operations
    void assignPermissionToRole(Long roleId, Long permissionId);
    void removePermissionFromRole(Long roleId, Long permissionId);
    Set<Permission> getRolePermissions(Long roleId);
    boolean roleHasPermission(Long roleId, String permissionName);
    
    // User-Role operations
    void assignRoleToUser(String username, Long roleId);
    void removeRoleFromUser(String username, Long roleId);
    Set<Role> getUserRoles(String username);
    Set<Permission> getUserPermissions(String username);
    boolean userHasPermission(String username, String permissionName);
    boolean userHasRole(String username, String roleName);
}

