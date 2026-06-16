package com.github.b3kt.application.service.impl;

import com.github.b3kt.application.properties.RbacProperties;
import com.github.b3kt.domain.model.Permission;
import com.github.b3kt.domain.model.Role;
import com.github.b3kt.infrastructure.persistence.entity.PermissionEntity;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.PermissionEntityRepository;
import com.github.b3kt.infrastructure.persistence.repository.RoleEntityRepository;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceImplTest {

    @Mock
    RbacProperties rbacProperties;

    @Mock
    RoleEntityRepository roleEntityRepository;

    @Mock
    PermissionEntityRepository permissionEntityRepository;

    @Mock
    UserEntityRepository userEntityRepository;

    @InjectMocks
    RbacServiceImpl rbacService;

    private RoleEntity testRoleEntity;
    private PermissionEntity testPermissionEntity;
    private UserEntity testUserEntity;

    @BeforeEach
    void setUp() {
        when(rbacProperties.enabled()).thenReturn(true);

        testRoleEntity = new RoleEntity("admin", "Admin role");
        testRoleEntity.setId(1L);

        testPermissionEntity = new PermissionEntity("read:users", "Read users", "users", "read");
        testPermissionEntity.setId(1L);

        testUserEntity = new UserEntity();
        testUserEntity.setUsername("testuser");
        testUserEntity.setRoles(new HashSet<>());
    }

    @Test
    @DisplayName("createRole creates and persists role")
    void createRole() {
        when(roleEntityRepository.existsByName("admin")).thenReturn(false);
        doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

        Role result = rbacService.createRole("admin", "Admin role");

        assertNotNull(result);
        assertEquals("admin", result.getName());
        verify(roleEntityRepository).persist(any(RoleEntity.class));
    }

    @Test
    @DisplayName("createRole throws on duplicate name")
    void createRoleDuplicate() {
        when(roleEntityRepository.existsByName("admin")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
            () -> rbacService.createRole("admin", "Admin role"));
    }

    @Test
    @DisplayName("updateRole updates name and description")
    void updateRole() {
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        when(roleEntityRepository.existsByName("superadmin")).thenReturn(false);
        doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

        Role result = rbacService.updateRole(1L, "superadmin", "Super admin");

        assertEquals("superadmin", result.getName());
        verify(roleEntityRepository).persist(any(RoleEntity.class));
    }

    @Test
    @DisplayName("updateRole throws when role not found")
    void updateRoleNotFound() {
        when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
            () -> rbacService.updateRole(999L, "name", "desc"));
    }

    @Test
    @DisplayName("updateRole with null name keeps existing name")
    void updateRoleNullName() {
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

        rbacService.updateRole(1L, null, "new desc");

        assertEquals("admin", testRoleEntity.getName());
    }

    @Test
    @DisplayName("getRoleById returns role")
    void getRoleById() {
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        Role result = rbacService.getRoleById(1L);
        assertNotNull(result);
    }

    @Test
    @DisplayName("getRoleById throws when not found")
    void getRoleByIdNotFound() {
        when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
            () -> rbacService.getRoleById(999L));
    }

    @Test
    @DisplayName("getRoleByName returns role")
    void getRoleByName() {
        when(roleEntityRepository.findByName("admin")).thenReturn(Optional.of(testRoleEntity));
        Role result = rbacService.getRoleByName("admin");
        assertNotNull(result);
    }

    @Test
    @DisplayName("getRoleByName throws when not found")
    void getRoleByNameNotFound() {
        when(roleEntityRepository.findByName("nonexistent")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
            () -> rbacService.getRoleByName("nonexistent"));
    }

    @Test
    @DisplayName("getAllRoles returns list")
    void getAllRoles() {
        when(roleEntityRepository.listAll()).thenReturn(List.of(testRoleEntity));
        List<Role> result = rbacService.getAllRoles();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getActiveRoles returns only active roles")
    void getActiveRoles() {
        when(roleEntityRepository.findActiveRoles()).thenReturn(List.of(testRoleEntity));
        List<Role> result = rbacService.getActiveRoles();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("deleteRole deletes existing role")
    void deleteRole() {
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        doNothing().when(roleEntityRepository).delete(any(RoleEntity.class));

        rbacService.deleteRole(1L);
        verify(roleEntityRepository).delete(any(RoleEntity.class));
    }

    @Test
    @DisplayName("deleteRole throws when not found")
    void deleteRoleNotFound() {
        when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
            () -> rbacService.deleteRole(999L));
    }

    @Test
    @DisplayName("activateRole sets active=true")
    void activateRole() {
        testRoleEntity.setActive(false);
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        rbacService.activateRole(1L);
        assertTrue(testRoleEntity.isActive());
    }

    @Test
    @DisplayName("deactivateRole sets active=false")
    void deactivateRole() {
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        rbacService.deactivateRole(1L);
        assertFalse(testRoleEntity.isActive());
    }

    @Test
    @DisplayName("createPermission creates and persists permission")
    void createPermission() {
        when(permissionEntityRepository.existsByName("read:users")).thenReturn(false);
        doNothing().when(permissionEntityRepository).persist(any(PermissionEntity.class));

        Permission result = rbacService.createPermission("read:users", "desc", "users", "read");
        assertNotNull(result);
    }

    @Test
    @DisplayName("createPermission throws on duplicate")
    void createPermissionDuplicate() {
        when(permissionEntityRepository.existsByName("read:users")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
            () -> rbacService.createPermission("read:users", "desc", "users", "read"));
    }

    @Test
    @DisplayName("updatePermission updates fields")
    void updatePermission() {
        when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testPermissionEntity));
        doNothing().when(permissionEntityRepository).persist(any(PermissionEntity.class));

        rbacService.updatePermission(1L, "write:users", "Write desc", "users", "write");
        assertEquals("write:users", testPermissionEntity.getName());
    }

    @Test
    @DisplayName("getAllPermissions returns list")
    void getAllPermissions() {
        when(permissionEntityRepository.listAll()).thenReturn(List.of(testPermissionEntity));
        List<Permission> result = rbacService.getAllPermissions();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("assignPermissionToRole adds permission to role")
    void assignPermissionToRole() {
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testPermissionEntity));
        rbacService.assignPermissionToRole(1L, 1L);
        assertTrue(testRoleEntity.getPermissions().contains(testPermissionEntity));
    }

    @Test
    @DisplayName("removePermissionFromRole removes permission")
    void removePermissionFromRole() {
        testRoleEntity.getPermissions().add(testPermissionEntity);
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testPermissionEntity));
        rbacService.removePermissionFromRole(1L, 1L);
        assertFalse(testRoleEntity.getPermissions().contains(testPermissionEntity));
    }

    @Test
    @DisplayName("getRolePermissions returns set of permissions")
    void getRolePermissions() {
        testRoleEntity.getPermissions().add(testPermissionEntity);
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        Set<Permission> result = rbacService.getRolePermissions(1L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("roleHasPermission returns true when permission exists and active")
    void roleHasPermission() {
        testPermissionEntity.setActive(true);
        testRoleEntity.getPermissions().add(testPermissionEntity);
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));

        assertTrue(rbacService.roleHasPermission(1L, "read:users"));
    }

    @Test
    @DisplayName("assignRoleToUser adds role to user")
    void assignRoleToUser() {
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testUserEntity));
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        rbacService.assignRoleToUser("testuser", 1L);
        assertTrue(testUserEntity.getRoles().contains(testRoleEntity));
    }

    @Test
    @DisplayName("removeRoleFromUser removes role from user")
    void removeRoleFromUser() {
        testUserEntity.getRoles().add(testRoleEntity);
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testUserEntity));
        when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(testRoleEntity));
        rbacService.removeRoleFromUser("testuser", 1L);
        assertFalse(testUserEntity.getRoles().contains(testRoleEntity));
    }

    @Test
    @DisplayName("getUserRoles returns set of roles")
    void getUserRoles() {
        testUserEntity.getRoles().add(testRoleEntity);
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testUserEntity));
        Set<Role> result = rbacService.getUserRoles("testuser");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getUserPermissions returns active permissions from active roles")
    void getUserPermissions() {
        testPermissionEntity.setActive(true);
        testRoleEntity.getPermissions().add(testPermissionEntity);
        testRoleEntity.setActive(true);
        testUserEntity.getRoles().add(testRoleEntity);
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testUserEntity));

        Set<Permission> result = rbacService.getUserPermissions("testuser");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getUserPermissions skips inactive roles")
    void getUserPermissionsInactiveRole() {
        testRoleEntity.setActive(false);
        testUserEntity.getRoles().add(testRoleEntity);
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testUserEntity));

        Set<Permission> result = rbacService.getUserPermissions("testuser");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("userHasPermission returns true when user has permission")
    void userHasPermission() {
        testPermissionEntity.setActive(true);
        testRoleEntity.getPermissions().add(testPermissionEntity);
        testRoleEntity.setActive(true);
        testUserEntity.getRoles().add(testRoleEntity);
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testUserEntity));

        assertTrue(rbacService.userHasPermission("testuser", "read:users"));
    }

    @Test
    @DisplayName("userHasRole returns true when user has active role")
    void userHasRole() {
        testRoleEntity.setActive(true);
        testUserEntity.getRoles().add(testRoleEntity);
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testUserEntity));

        assertTrue(rbacService.userHasRole("testuser", "admin"));
    }

    @Test
    @DisplayName("userHasRole returns false for inactive role")
    void userHasRoleInactive() {
        testRoleEntity.setActive(false);
        testUserEntity.getRoles().add(testRoleEntity);
        when(userEntityRepository.findByUsername("testuser")).thenReturn(Optional.of(testUserEntity));

        assertFalse(rbacService.userHasRole("testuser", "admin"));
    }

    @Test
    @DisplayName("all operations throw when RBAC is disabled")
    void rbacDisabled() {
        when(rbacProperties.enabled()).thenReturn(false);

        assertThrows(IllegalStateException.class,
            () -> rbacService.createRole("admin", "desc"));
        assertThrows(IllegalStateException.class,
            () -> rbacService.getAllRoles());
        assertThrows(IllegalStateException.class,
            () -> rbacService.createPermission("p", "d", "r", "a"));
    }
}
