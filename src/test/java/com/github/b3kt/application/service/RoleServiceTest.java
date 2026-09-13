package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.RoleEntityRepository;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleServiceTest {

    @Mock
    RoleEntityRepository repository;

    @Mock
    UserEntityRepository userRepository;

    @Mock
    PanacheQuery<RoleEntity> roleQuery;

    @Mock
    PanacheQuery<UserEntity> userQuery;

    @Mock
    PanacheQuery<RoleEntity> sortedQuery;

    @InjectMocks
    RoleService roleService;

    private RoleEntity testRole;

    @BeforeEach
    void setUp() {
        testRole = new RoleEntity("admin", "Admin role");
        testRole.setId(1L);
    }

    @Nested
    @DisplayName("AbstractCrudService methods")
    class CrudTests {

        @Test
        @DisplayName("findAll returns all roles")
        void findAll() {
            when(repository.listAll()).thenReturn(List.of(testRole));
            assertEquals(1, roleService.findAll().size());
        }

        @Test
        @DisplayName("findById returns role")
        void findById() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testRole));
            assertNotNull(roleService.findById(1L));
        }

        @Test
        @DisplayName("findById throws when not found")
        void findByIdNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> roleService.findById(999L));
        }

        @Test
        @DisplayName("create persists and returns role")
        void create() {
            doNothing().when(repository).persist(any(RoleEntity.class));
            assertNotNull(roleService.create(testRole));
        }

        @Test
        @DisplayName("update merges entity")
        void update() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testRole));
            when(repository.getEntityManager()).thenReturn(mock(jakarta.persistence.EntityManager.class));
            when(repository.getEntityManager().merge(any(RoleEntity.class))).thenReturn(testRole);

            RoleEntity result = roleService.update(1L, testRole);
            assertNotNull(result);
        }

        @Test
        @DisplayName("update throws when not found")
        void updateNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> roleService.update(999L, testRole));
        }

        @Test
        @DisplayName("delete removes entity")
        void delete() {
            when(repository.deleteById(1L)).thenReturn(true);
            roleService.delete(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("delete throws when not found")
        void deleteNotFound() {
            when(repository.deleteById(999L)).thenThrow(new RuntimeException("not found"));
            assertThrows(EntityNotFoundException.class, () -> roleService.delete(999L));
        }
    }

    @Nested
    @DisplayName("findPaginated")
    class FindPaginatedTests {

        @Test
        @DisplayName("with search")
        void findPaginatedWithSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("admin");
            when(repository.find(anyString(), anyString())).thenReturn(roleQuery);
            when(roleQuery.count()).thenReturn(1L);
            when(roleQuery.page(any(Page.class))).thenReturn(roleQuery);
            when(roleQuery.list()).thenReturn(List.of(testRole));

            PageResponse<RoleEntity> result = roleService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("without search")
        void findPaginatedNoSearch() {
            PageRequest pr = new PageRequest(1, 10);
            when(repository.findAll()).thenReturn(roleQuery);
            when(roleQuery.count()).thenReturn(1L);
            when(roleQuery.page(any(Page.class))).thenReturn(roleQuery);
            when(roleQuery.list()).thenReturn(List.of(testRole));

            PageResponse<RoleEntity> result = roleService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with sort and search")
        void findPaginatedWithSortAndSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("admin");
            pr.setSortBy("name");
            pr.setDescending(true);
            when(repository.find(anyString(), anyString())).thenReturn(roleQuery);
            when(roleQuery.page(any(Page.class))).thenReturn(roleQuery);
            when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(testRole));

            PageResponse<RoleEntity> result = roleService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with sort and no search")
        void findPaginatedWithSortNoSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSortBy("name");
            when(repository.findAll()).thenReturn(roleQuery);
            when(roleQuery.page(any(Page.class))).thenReturn(roleQuery);
            when(repository.findAll(any(Sort.class))).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(testRole));

            PageResponse<RoleEntity> result = roleService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }
    }

    @Nested
    @DisplayName("getUsersByRoleId")
    class GetUsersByRoleIdTests {

        @Test
        @DisplayName("returns users with that role")
        void getUsersByRoleId() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testRole));
            when(userRepository.find(anyString(), anyLong())).thenReturn(userQuery);
            when(userQuery.list()).thenReturn(List.of(new UserEntity()));

            List<UserEntity> users = roleService.getUsersByRoleId(1L);
            assertEquals(1, users.size());
        }

        @Test
        @DisplayName("throws when role not found")
        void getUsersByRoleIdNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> roleService.getUsersByRoleId(999L));
        }
    }

    @Nested
    @DisplayName("updateRoleUsers")
    class UpdateRoleUsersTests {

        @Test
        @DisplayName("replaces user assignments")
        void updateRoleUsers() {
            UserEntity existingUser = new UserEntity();
            existingUser.setId(1L);
            existingUser.setRoles(new HashSet<>());
            existingUser.getRoles().add(testRole);

            UserEntity newUser = new UserEntity();
            newUser.setId(2L);
            newUser.setRoles(new HashSet<>());

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testRole));
            when(userRepository.find(anyString(), anyLong())).thenReturn(userQuery);
            when(userQuery.list()).thenReturn(List.of(existingUser));
            when(userRepository.findByIdOptional(2L)).thenReturn(Optional.of(newUser));

            roleService.updateRoleUsers(1L, List.of(2L));

            assertFalse(existingUser.getRoles().contains(testRole));
            assertTrue(newUser.getRoles().contains(testRole));
        }

        @Test
        @DisplayName("handles empty userIds")
        void updateRoleUsers_emptyList() {
            UserEntity existingUser = new UserEntity();
            existingUser.setId(1L);
            existingUser.setRoles(new HashSet<>());
            existingUser.getRoles().add(testRole);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testRole));
            when(userRepository.find(anyString(), anyLong())).thenReturn(userQuery);
            when(userQuery.list()).thenReturn(List.of(existingUser));

            roleService.updateRoleUsers(1L, List.of());

            assertFalse(existingUser.getRoles().contains(testRole));
        }

        @Test
        @DisplayName("handles null userIds")
        void updateRoleUsers_nullList() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testRole));
            when(userRepository.find(anyString(), anyLong())).thenReturn(userQuery);
            when(userQuery.list()).thenReturn(List.of());

            roleService.updateRoleUsers(1L, null);
        }

        @Test
        @DisplayName("throws when role not found")
        void updateRoleUsers_roleNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> roleService.updateRoleUsers(999L, List.of()));
        }

        @Test
        @DisplayName("throws when user not found")
        void updateRoleUsers_userNotFound() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testRole));
            when(userRepository.find(anyString(), anyLong())).thenReturn(userQuery);
            when(userQuery.list()).thenReturn(List.of());
            when(userRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> roleService.updateRoleUsers(1L, List.of(999L)));
        }

        @Test
        @DisplayName("creates roles set if null")
        void updateRoleUsers_nullRolesSet() {
            UserEntity newUser = new UserEntity();
            newUser.setId(2L);
            newUser.setRoles(null);

            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testRole));
            when(userRepository.find(anyString(), anyLong())).thenReturn(userQuery);
            when(userQuery.list()).thenReturn(List.of());
            when(userRepository.findByIdOptional(2L)).thenReturn(Optional.of(newUser));

            roleService.updateRoleUsers(1L, List.of(2L));

            assertNotNull(newUser.getRoles());
            assertTrue(newUser.getRoles().contains(testRole));
        }
    }
}
