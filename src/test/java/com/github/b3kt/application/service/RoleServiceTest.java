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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    RoleEntityRepository repository;

    @Mock
    UserEntityRepository userRepository;

    @Mock
    PanacheQuery<RoleEntity> roleQuery;

    @Mock
    PanacheQuery<UserEntity> userQuery;

    @InjectMocks
    RoleService roleService;

    private RoleEntity testRole;

    @BeforeEach
    void setUp() {
        testRole = new RoleEntity("admin", "Admin role");
        testRole.setId(1L);
    }

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
    @DisplayName("findPaginated with search")
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
    @DisplayName("findPaginated without search")
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
    @DisplayName("findPaginated with sort and search")
    void findPaginatedWithSortAndSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("admin");
        pr.setSortBy("name");
        when(repository.find(anyString(), anyString())).thenReturn(roleQuery);
        when(roleQuery.page(any(Page.class))).thenReturn(roleQuery);
        when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(roleQuery);
        when(roleQuery.count()).thenReturn(1L);
        when(roleQuery.list()).thenReturn(List.of(testRole));

        PageResponse<RoleEntity> result = roleService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("getUsersByRoleId returns users with that role")
    void getUsersByRoleId() {
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testRole));
        when(userRepository.find(anyString(), anyLong())).thenReturn(userQuery);
        when(userQuery.list()).thenReturn(List.of(new UserEntity()));

        List<UserEntity> users = roleService.getUsersByRoleId(1L);
        assertEquals(1, users.size());
    }

    @Test
    @DisplayName("getUsersByRoleId throws when role not found")
    void getUsersByRoleIdNotFound() {
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
            () -> roleService.getUsersByRoleId(999L));
    }

    @Test
    @DisplayName("updateRoleUsers replaces user assignments")
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
    @DisplayName("create persists and returns role")
    void create() {
        doNothing().when(repository).persist(any(RoleEntity.class));
        assertNotNull(roleService.create(testRole));
    }
}
