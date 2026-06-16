package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.PermissionEntity;
import com.github.b3kt.infrastructure.persistence.repository.PermissionEntityRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    PermissionEntityRepository repository;

    @Mock
    PanacheQuery<PermissionEntity> query;

    @InjectMocks
    PermissionService permissionService;

    private PermissionEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new PermissionEntity("read:users", "Read users", "users", "read");
        testEntity.setId(1L);
    }

    @Test
    @DisplayName("findAll returns all permissions")
    void findAll() {
        when(repository.listAll()).thenReturn(List.of(testEntity));
        assertEquals(1, permissionService.findAll().size());
    }

    @Test
    @DisplayName("findById returns permission")
    void findById() {
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
        assertNotNull(permissionService.findById(1L));
    }

    @Test
    @DisplayName("findPaginated with search")
    void findPaginatedWithSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("read");
        when(repository.find(anyString(), anyString())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<PermissionEntity> result = permissionService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated without search")
    void findPaginatedNoSearch() {
        PageRequest pr = new PageRequest(1, 10);
        when(repository.findAll()).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<PermissionEntity> result = permissionService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated with sort and search")
    void findPaginatedWithSortAndSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("read");
        pr.setSortBy("name");
        when(repository.find(anyString(), anyString())).thenReturn(query);
        when(query.page(any(Page.class))).thenReturn(query);
        when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<PermissionEntity> result = permissionService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("create persists and returns entity")
    void create() {
        doNothing().when(repository).persist(any(PermissionEntity.class));
        assertNotNull(permissionService.create(testEntity));
    }
}
