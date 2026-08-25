package com.github.b3kt.application.service.impl;

import com.github.b3kt.application.properties.RbacProperties;
import com.github.b3kt.domain.exception.UserNotFoundException;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RbacServiceImpl Tests")
class RbacServiceImplTest {

    @Mock
    private RbacProperties rbacProperties;

    @Mock
    private RoleEntityRepository roleEntityRepository;

    @Mock
    private PermissionEntityRepository permissionEntityRepository;

    @Mock
    private UserEntityRepository userEntityRepository;

    private RbacServiceImpl rbacService;

    @BeforeEach
    void setUp() {
        rbacService = new RbacServiceImpl();
        rbacService.rbacProperties = rbacProperties;
        rbacService.roleEntityRepository = roleEntityRepository;
        rbacService.permissionEntityRepository = permissionEntityRepository;
        rbacService.userEntityRepository = userEntityRepository;
    }

    private void enableRbac() {
        when(rbacProperties.enabled()).thenReturn(true);
    }

    private RoleEntity createRoleEntity(Long id, String name) {
        RoleEntity entity = new RoleEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setActive(true);
        entity.setPermissions(new HashSet<>());
        return entity;
    }

    private PermissionEntity createPermissionEntity(Long id, String name, String resource, String action) {
        PermissionEntity entity = new PermissionEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setResource(resource);
        entity.setAction(action);
        entity.setActive(true);
        return entity;
    }

    private UserEntity createUserEntity(String username) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername(username);
        user.setActive(true);
        user.setRoles(new HashSet<>());
        return user;
    }

    @Nested
    @DisplayName("RBAC disabled")
    class DisabledTests {

        @Test
        @DisplayName("Should throw IllegalStateException when RBAC disabled for createRole")
        void testCreateRole_disabled() {
            when(rbacProperties.enabled()).thenReturn(false);
            assertThrows(IllegalStateException.class, () -> rbacService.createRole("admin", "desc"));
        }

        @Test
        @DisplayName("Should throw IllegalStateException for any method when disabled")
        void testAnyMethod_disabled() {
            when(rbacProperties.enabled()).thenReturn(false);
            assertThrows(IllegalStateException.class, () -> rbacService.getAllRoles());
            assertThrows(IllegalStateException.class, () -> rbacService.getAllPermissions());
            assertThrows(IllegalStateException.class, () -> rbacService.getUserRoles("user"));
        }
    }

    @Nested
    @DisplayName("Role operations")
    class RoleTests {

        @Test
        @DisplayName("Should create role")
        void testCreateRole() {
            enableRbac();
            when(roleEntityRepository.existsByName("admin")).thenReturn(false);
            doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

            Role result = rbacService.createRole("admin", "Administrator");

            assertNotNull(result);
            assertEquals("admin", result.getName());
        }

        @Test
        @DisplayName("Should throw when role name exists")
        void testCreateRole_duplicate() {
            enableRbac();
            when(roleEntityRepository.existsByName("admin")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> rbacService.createRole("admin", "desc"));
        }

        @Test
        @DisplayName("Should update role name")
        void testUpdateRole() {
            enableRbac();
            RoleEntity existing = createRoleEntity(1L, "old_name");
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(existing));
            when(roleEntityRepository.existsByName("new_name")).thenReturn(false);
            doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

            Role result = rbacService.updateRole(1L, "new_name", "new desc");

            assertEquals("new_name", result.getName());
        }

        @Test
        @DisplayName("Should throw when updating to existing name")
        void testUpdateRole_duplicateName() {
            enableRbac();
            RoleEntity existing = createRoleEntity(1L, "old_name");
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(existing));
            when(roleEntityRepository.existsByName("taken")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> rbacService.updateRole(1L, "taken", null));
        }

        @Test
        @DisplayName("Should not change name when null")
        void testUpdateRole_nullName() {
            enableRbac();
            RoleEntity existing = createRoleEntity(1L, "keep_name");
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(existing));
            doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

            Role result = rbacService.updateRole(1L, null, "new desc");

            assertEquals("keep_name", result.getName());
        }

        @Test
        @DisplayName("Should get role by ID")
        void testGetRoleById() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(createRoleEntity(1L, "admin")));

            Role result = rbacService.getRoleById(1L);

            assertEquals("admin", result.getName());
        }

        @Test
        @DisplayName("Should throw when role not found by ID")
        void testGetRoleById_notFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.getRoleById(999L));
        }

        @Test
        @DisplayName("Should get role by name")
        void testGetRoleByName() {
            enableRbac();
            when(roleEntityRepository.findByName("admin")).thenReturn(Optional.of(createRoleEntity(1L, "admin")));

            Role result = rbacService.getRoleByName("admin");

            assertEquals("admin", result.getName());
        }

        @Test
        @DisplayName("Should throw when role not found by name")
        void testGetRoleByName_notFound() {
            enableRbac();
            when(roleEntityRepository.findByName("nonexistent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.getRoleByName("nonexistent"));
        }

        @Test
        @DisplayName("Should get all roles")
        void testGetAllRoles() {
            enableRbac();
            when(roleEntityRepository.listAll()).thenReturn(List.of(createRoleEntity(1L, "admin")));

            List<Role> result = rbacService.getAllRoles();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should get active roles")
        void testGetActiveRoles() {
            enableRbac();
            when(roleEntityRepository.findActiveRoles()).thenReturn(List.of(createRoleEntity(1L, "admin")));

            List<Role> result = rbacService.getActiveRoles();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should delete role")
        void testDeleteRole() {
            enableRbac();
            RoleEntity entity = createRoleEntity(1L, "admin");
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(entity));

            rbacService.deleteRole(1L);

            verify(roleEntityRepository).delete(entity);
        }

        @Test
        @DisplayName("Should throw when deleting nonexistent role")
        void testDeleteRole_notFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.deleteRole(999L));
        }

        @Test
        @DisplayName("Should activate role")
        void testActivateRole() {
            enableRbac();
            RoleEntity entity = createRoleEntity(1L, "admin");
            entity.setActive(false);
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(entity));
            doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

            rbacService.activateRole(1L);

            assertTrue(entity.isActive());
        }

        @Test
        @DisplayName("Should deactivate role")
        void testDeactivateRole() {
            enableRbac();
            RoleEntity entity = createRoleEntity(1L, "admin");
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(entity));
            doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

            rbacService.deactivateRole(1L);

            assertFalse(entity.isActive());
        }

        @Test
        @DisplayName("Should throw when updating nonexistent role")
        void testUpdateRole_notFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.updateRole(999L, "new", "desc"));
        }

        @Test
        @DisplayName("Should not change name when same as existing")
        void testUpdateRole_sameName() {
            enableRbac();
            RoleEntity existing = createRoleEntity(1L, "admin");
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(existing));
            doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

            Role result = rbacService.updateRole(1L, "admin", "new desc");

            assertEquals("admin", result.getName());
            verify(roleEntityRepository, never()).existsByName(anyString());
        }

        @Test
        @DisplayName("Should throw when activating nonexistent role")
        void testActivateRole_notFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.activateRole(999L));
        }

        @Test
        @DisplayName("Should throw when deactivating nonexistent role")
        void testDeactivateRole_notFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.deactivateRole(999L));
        }
    }

    @Nested
    @DisplayName("Permission operations")
    class PermissionTests {

        @Test
        @DisplayName("Should create permission")
        void testCreatePermission() {
            enableRbac();
            when(permissionEntityRepository.existsByName("read")).thenReturn(false);
            doNothing().when(permissionEntityRepository).persist(any(PermissionEntity.class));

            Permission result = rbacService.createPermission("read", "Read access", "resource", "read");

            assertNotNull(result);
            assertEquals("read", result.getName());
        }

        @Test
        @DisplayName("Should throw when permission name exists")
        void testCreatePermission_duplicate() {
            enableRbac();
            when(permissionEntityRepository.existsByName("read")).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> rbacService.createPermission("read", "desc", "res", "act"));
        }

        @Test
        @DisplayName("Should update permission")
        void testUpdatePermission() {
            enableRbac();
            PermissionEntity existing = createPermissionEntity(1L, "old", "old_res", "old_act");
            when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(existing));
            when(permissionEntityRepository.existsByName("new")).thenReturn(false);
            doNothing().when(permissionEntityRepository).persist(any(PermissionEntity.class));

            Permission result = rbacService.updatePermission(1L, "new", "new desc", "new_res", "new_act");

            assertEquals("new", result.getName());
        }

        @Test
        @DisplayName("Should not change name when null in update")
        void testUpdatePermission_nullName() {
            enableRbac();
            PermissionEntity existing = createPermissionEntity(1L, "keep", "res", "act");
            when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(existing));
            doNothing().when(permissionEntityRepository).persist(any(PermissionEntity.class));

            Permission result = rbacService.updatePermission(1L, null, null, null, null);

            assertEquals("keep", result.getName());
        }

        @Test
        @DisplayName("Should throw when updating to existing permission name")
        void testUpdatePermission_duplicateName() {
            enableRbac();
            PermissionEntity existing = createPermissionEntity(1L, "old", "res", "act");
            when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(existing));
            when(permissionEntityRepository.existsByName("taken")).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> rbacService.updatePermission(1L, "taken", null, null, null));
        }

        @Test
        @DisplayName("Should get permission by ID")
        void testGetPermissionById() {
            enableRbac();
            when(permissionEntityRepository.findByIdOptional(1L))
                    .thenReturn(Optional.of(createPermissionEntity(1L, "read", "res", "read")));

            Permission result = rbacService.getPermissionById(1L);

            assertEquals("read", result.getName());
        }

        @Test
        @DisplayName("Should throw when permission not found by ID")
        void testGetPermissionById_notFound() {
            enableRbac();
            when(permissionEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.getPermissionById(999L));
        }

        @Test
        @DisplayName("Should get permission by name")
        void testGetPermissionByName() {
            enableRbac();
            when(permissionEntityRepository.findByName("read"))
                    .thenReturn(Optional.of(createPermissionEntity(1L, "read", "res", "read")));

            Permission result = rbacService.getPermissionByName("read");

            assertEquals("read", result.getName());
        }

        @Test
        @DisplayName("Should throw when permission not found by name")
        void testGetPermissionByName_notFound() {
            enableRbac();
            when(permissionEntityRepository.findByName("nonexistent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.getPermissionByName("nonexistent"));
        }

        @Test
        @DisplayName("Should get all permissions")
        void testGetAllPermissions() {
            enableRbac();
            when(permissionEntityRepository.listAll())
                    .thenReturn(List.of(createPermissionEntity(1L, "read", "res", "read")));

            List<Permission> result = rbacService.getAllPermissions();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should get active permissions")
        void testGetActivePermissions() {
            enableRbac();
            when(permissionEntityRepository.findActivePermissions())
                    .thenReturn(List.of(createPermissionEntity(1L, "read", "res", "read")));

            List<Permission> result = rbacService.getActivePermissions();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should get permissions by resource")
        void testGetPermissionsByResource() {
            enableRbac();
            when(permissionEntityRepository.findByResource("spk"))
                    .thenReturn(List.of(createPermissionEntity(1L, "read_spk", "spk", "read")));

            List<Permission> result = rbacService.getPermissionsByResource("spk");

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should delete permission")
        void testDeletePermission() {
            enableRbac();
            PermissionEntity entity = createPermissionEntity(1L, "read", "res", "read");
            when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(entity));

            rbacService.deletePermission(1L);

            verify(permissionEntityRepository).delete(entity);
        }

        @Test
        @DisplayName("Should throw when deleting nonexistent permission")
        void testDeletePermission_notFound() {
            enableRbac();
            when(permissionEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.deletePermission(999L));
        }

        @Test
        @DisplayName("Should activate permission")
        void testActivatePermission() {
            enableRbac();
            PermissionEntity entity = createPermissionEntity(1L, "read", "res", "read");
            entity.setActive(false);
            when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(entity));
            doNothing().when(permissionEntityRepository).persist(any(PermissionEntity.class));

            rbacService.activatePermission(1L);

            assertTrue(entity.isActive());
        }

        @Test
        @DisplayName("Should deactivate permission")
        void testDeactivatePermission() {
            enableRbac();
            PermissionEntity entity = createPermissionEntity(1L, "read", "res", "read");
            when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(entity));
            doNothing().when(permissionEntityRepository).persist(any(PermissionEntity.class));

            rbacService.deactivatePermission(1L);

            assertFalse(entity.isActive());
        }

        @Test
        @DisplayName("Should not change name when same as existing")
        void testUpdatePermission_sameName() {
            enableRbac();
            PermissionEntity existing = createPermissionEntity(1L, "read", "res", "act");
            when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(existing));
            doNothing().when(permissionEntityRepository).persist(any(PermissionEntity.class));

            Permission result = rbacService.updatePermission(1L, "read", "new desc", null, null);

            assertEquals("read", result.getName());
            verify(permissionEntityRepository, never()).existsByName(anyString());
        }
    }

    @Nested
    @DisplayName("Role-Permission operations")
    class RolePermissionTests {

        @Test
        @DisplayName("Should assign permission to role")
        void testAssignPermissionToRole() {
            enableRbac();
            RoleEntity role = createRoleEntity(1L, "admin");
            PermissionEntity perm = createPermissionEntity(1L, "read", "res", "read");
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(role));
            when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(perm));
            doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

            rbacService.assignPermissionToRole(1L, 1L);

            assertTrue(role.getPermissions().contains(perm));
        }

        @Test
        @DisplayName("Should remove permission from role")
        void testRemovePermissionFromRole() {
            enableRbac();
            PermissionEntity perm = createPermissionEntity(1L, "read", "res", "read");
            RoleEntity role = createRoleEntity(1L, "admin");
            role.getPermissions().add(perm);
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(role));
            when(permissionEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(perm));
            doNothing().when(roleEntityRepository).persist(any(RoleEntity.class));

            rbacService.removePermissionFromRole(1L, 1L);

            assertFalse(role.getPermissions().contains(perm));
        }

        @Test
        @DisplayName("Should get role permissions")
        void testGetRolePermissions() {
            enableRbac();
            PermissionEntity perm = createPermissionEntity(1L, "read", "res", "read");
            RoleEntity role = createRoleEntity(1L, "admin");
            role.getPermissions().add(perm);
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(role));

            Set<Permission> result = rbacService.getRolePermissions(1L);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should check if role has permission")
        void testRoleHasPermission() {
            enableRbac();
            PermissionEntity perm = createPermissionEntity(1L, "read", "res", "read");
            RoleEntity role = createRoleEntity(1L, "admin");
            role.getPermissions().add(perm);
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(role));

            assertTrue(rbacService.roleHasPermission(1L, "read"));
            assertFalse(rbacService.roleHasPermission(1L, "write"));
        }

        @Test
        @DisplayName("Should check inactive permission not matched")
        void testRoleHasPermission_inactive() {
            enableRbac();
            PermissionEntity perm = createPermissionEntity(1L, "read", "res", "read");
            perm.setActive(false);
            RoleEntity role = createRoleEntity(1L, "admin");
            role.getPermissions().add(perm);
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(role));

            assertFalse(rbacService.roleHasPermission(1L, "read"));
        }

        @Test
        @DisplayName("Should throw when role not found for permission assignment")
        void testAssignPermission_roleNotFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.assignPermissionToRole(999L, 1L));
        }

        @Test
        @DisplayName("Should throw when permission not found for assignment")
        void testAssignPermission_permissionNotFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(createRoleEntity(1L, "admin")));
            when(permissionEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.assignPermissionToRole(1L, 999L));
        }

        @Test
        @DisplayName("Should throw when role not found for permission removal")
        void testRemovePermission_roleNotFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.removePermissionFromRole(999L, 1L));
        }

        @Test
        @DisplayName("Should throw when permission not found for removal")
        void testRemovePermission_permissionNotFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(createRoleEntity(1L, "admin")));
            when(permissionEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.removePermissionFromRole(1L, 999L));
        }

        @Test
        @DisplayName("Should throw when getting permissions for nonexistent role")
        void testGetRolePermissions_roleNotFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.getRolePermissions(999L));
        }

        @Test
        @DisplayName("Should throw when checking permission for nonexistent role")
        void testRoleHasPermission_roleNotFound() {
            enableRbac();
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.roleHasPermission(999L, "read"));
        }

        @Test
        @DisplayName("Should handle multiple roles with mixed active/inactive permissions")
        void testGetUserPermissions_mixedRolesAndPermissions() {
            enableRbac();
            PermissionEntity activePerm = createPermissionEntity(1L, "read", "res", "read");
            PermissionEntity inactivePerm = createPermissionEntity(2L, "write", "res", "write");
            inactivePerm.setActive(false);

            RoleEntity activeRole = createRoleEntity(1L, "admin");
            activeRole.getPermissions().add(activePerm);
            activeRole.getPermissions().add(inactivePerm);

            RoleEntity inactiveRole = createRoleEntity(2L, "viewer");
            inactiveRole.setActive(false);
            inactiveRole.getPermissions().add(createPermissionEntity(3L, "view", "res", "view"));

            UserEntity user = createUserEntity("admin");
            user.getRoles().add(activeRole);
            user.getRoles().add(inactiveRole);
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            Set<Permission> result = rbacService.getUserPermissions("admin");

            assertEquals(1, result.size());
            assertTrue(result.stream().anyMatch(p -> p.getName().equals("read")));
        }
    }

    @Nested
    @DisplayName("User-Role operations")
    class UserRoleTests {

        @Test
        @DisplayName("Should assign role to user")
        void testAssignRoleToUser() {
            enableRbac();
            UserEntity user = createUserEntity("admin");
            RoleEntity role = createRoleEntity(1L, "admin");
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(role));
            doNothing().when(userEntityRepository).persist(any(UserEntity.class));

            rbacService.assignRoleToUser("admin", 1L);

            assertTrue(user.getRoles().contains(role));
        }

        @Test
        @DisplayName("Should throw when user not found for role assignment")
        void testAssignRoleToUser_userNotFound() {
            enableRbac();
            when(userEntityRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> rbacService.assignRoleToUser("nonexistent", 1L));
        }

        @Test
        @DisplayName("Should throw when role not found for user assignment")
        void testAssignRoleToUser_roleNotFound() {
            enableRbac();
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(createUserEntity("admin")));
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.assignRoleToUser("admin", 999L));
        }

        @Test
        @DisplayName("Should remove role from user")
        void testRemoveRoleFromUser() {
            enableRbac();
            RoleEntity role = createRoleEntity(1L, "admin");
            UserEntity user = createUserEntity("admin");
            user.getRoles().add(role);
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));
            when(roleEntityRepository.findByIdOptional(1L)).thenReturn(Optional.of(role));
            doNothing().when(userEntityRepository).persist(any(UserEntity.class));

            rbacService.removeRoleFromUser("admin", 1L);

            assertFalse(user.getRoles().contains(role));
        }

        @Test
        @DisplayName("Should throw when user not found for role removal")
        void testRemoveRoleFromUser_userNotFound() {
            enableRbac();
            when(userEntityRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> rbacService.removeRoleFromUser("nonexistent", 1L));
        }

        @Test
        @DisplayName("Should get user roles")
        void testGetUserRoles() {
            enableRbac();
            RoleEntity role = createRoleEntity(1L, "admin");
            UserEntity user = createUserEntity("admin");
            user.getRoles().add(role);
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            Set<Role> result = rbacService.getUserRoles("admin");

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should throw when user not found for getting roles")
        void testGetUserRoles_userNotFound() {
            enableRbac();
            when(userEntityRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> rbacService.getUserRoles("nonexistent"));
        }

        @Test
        @DisplayName("Should get user permissions")
        void testGetUserPermissions() {
            enableRbac();
            PermissionEntity perm = createPermissionEntity(1L, "read", "res", "read");
            RoleEntity role = createRoleEntity(1L, "admin");
            role.getPermissions().add(perm);
            UserEntity user = createUserEntity("admin");
            user.getRoles().add(role);
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            Set<Permission> result = rbacService.getUserPermissions("admin");

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should skip inactive roles when getting user permissions")
        void testGetUserPermissions_inactiveRole() {
            enableRbac();
            RoleEntity role = createRoleEntity(1L, "admin");
            role.setActive(false);
            UserEntity user = createUserEntity("admin");
            user.getRoles().add(role);
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            Set<Permission> result = rbacService.getUserPermissions("admin");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should skip inactive permissions when getting user permissions")
        void testGetUserPermissions_inactivePermission() {
            enableRbac();
            PermissionEntity perm = createPermissionEntity(1L, "read", "res", "read");
            perm.setActive(false);
            RoleEntity role = createRoleEntity(1L, "admin");
            role.getPermissions().add(perm);
            UserEntity user = createUserEntity("admin");
            user.getRoles().add(role);
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            Set<Permission> result = rbacService.getUserPermissions("admin");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should check user has permission")
        void testUserHasPermission() {
            enableRbac();
            PermissionEntity perm = createPermissionEntity(1L, "read", "res", "read");
            RoleEntity role = createRoleEntity(1L, "admin");
            role.getPermissions().add(perm);
            UserEntity user = createUserEntity("admin");
            user.getRoles().add(role);
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            assertTrue(rbacService.userHasPermission("admin", "read"));
            assertFalse(rbacService.userHasPermission("admin", "write"));
        }

        @Test
        @DisplayName("Should check user has role")
        void testUserHasRole() {
            enableRbac();
            RoleEntity role = createRoleEntity(1L, "admin");
            UserEntity user = createUserEntity("admin");
            user.getRoles().add(role);
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            assertTrue(rbacService.userHasRole("admin", "admin"));
            assertFalse(rbacService.userHasRole("admin", "user"));
        }

        @Test
        @DisplayName("Should not match inactive role in userHasRole")
        void testUserHasRole_inactive() {
            enableRbac();
            RoleEntity role = createRoleEntity(1L, "admin");
            role.setActive(false);
            UserEntity user = createUserEntity("admin");
            user.getRoles().add(role);
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            assertFalse(rbacService.userHasRole("admin", "admin"));
        }

        @Test
        @DisplayName("Should throw when user not found for userHasPermission")
        void testUserHasPermission_userNotFound() {
            enableRbac();
            when(userEntityRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> rbacService.userHasPermission("nonexistent", "read"));
        }

        @Test
        @DisplayName("Should throw when user not found for userHasRole")
        void testUserHasRole_userNotFound() {
            enableRbac();
            when(userEntityRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> rbacService.userHasRole("nonexistent", "admin"));
        }

        @Test
        @DisplayName("Should throw when role not found for removal from user")
        void testRemoveRoleFromUser_roleNotFound() {
            enableRbac();
            UserEntity user = createUserEntity("admin");
            when(userEntityRepository.findByUsername("admin")).thenReturn(Optional.of(user));
            when(roleEntityRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> rbacService.removeRoleFromUser("admin", 999L));
        }
    }
}
