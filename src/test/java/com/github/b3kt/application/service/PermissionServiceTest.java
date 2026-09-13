package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.PermissionEntity;
import com.github.b3kt.infrastructure.persistence.repository.PermissionEntityRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionServiceTest {

    @Mock
    PermissionEntityRepository repository;

    @Mock
    PanacheQuery<PermissionEntity> query;

    @Mock
    PanacheQuery<PermissionEntity> sortedQuery;

    @InjectMocks
    PermissionService permissionService;

    private PermissionEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new PermissionEntity("read:users", "Read users", "users", "read");
        testEntity.setId(1L);
    }

    @Nested
    @DisplayName("AbstractCrudService methods")
    class CrudTests {

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
        @DisplayName("findById throws when not found")
        void findByIdNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> permissionService.findById(999L));
        }

        @Test
        @DisplayName("create persists and returns entity")
        void create() {
            doNothing().when(repository).persist(any(PermissionEntity.class));
            assertNotNull(permissionService.create(testEntity));
        }

        @Test
        @DisplayName("update merges entity")
        void update() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            when(repository.getEntityManager()).thenReturn(mock(jakarta.persistence.EntityManager.class));
            when(repository.getEntityManager().merge(any(PermissionEntity.class))).thenReturn(testEntity);

            PermissionEntity result = permissionService.update(1L, testEntity);
            assertNotNull(result);
        }

        @Test
        @DisplayName("update throws when not found")
        void updateNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> permissionService.update(999L, testEntity));
        }

        @Test
        @DisplayName("delete removes entity")
        void delete() {
            when(repository.deleteById(1L)).thenReturn(true);
            permissionService.delete(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("delete throws when not found")
        void deleteNotFound() {
            when(repository.deleteById(999L)).thenThrow(new RuntimeException("not found"));
            assertThrows(EntityNotFoundException.class, () -> permissionService.delete(999L));
        }
    }

    @Nested
    @DisplayName("findPaginated")
    class FindPaginatedTests {

        @Test
        @DisplayName("with search")
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
        @DisplayName("without search")
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
        @DisplayName("with sort and search")
        void findPaginatedWithSortAndSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("read");
            pr.setSortBy("name");
            when(repository.find(anyString(), anyString())).thenReturn(query);
            when(query.page(any(Page.class))).thenReturn(query);
            when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<PermissionEntity> result = permissionService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with sort and no search")
        void findPaginatedWithSortNoSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSortBy("name");
            when(repository.findAll()).thenReturn(query);
            when(query.page(any(Page.class))).thenReturn(query);
            when(repository.findAll(any(Sort.class))).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<PermissionEntity> result = permissionService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with sort and search - ascending")
        void findPaginatedWithSortAndSearch_ascending() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("read");
            pr.setSortBy("name");
            pr.setDescending(false);
            when(repository.find(anyString(), anyString())).thenReturn(query);
            when(query.page(any(Page.class))).thenReturn(query);
            when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(new com.github.b3kt.infrastructure.persistence.entity.PermissionEntity()));

            var result = permissionService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with sort and no search - ascending")
        void findPaginatedWithSortNoSearch_ascending() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSortBy("name");
            pr.setDescending(false);
            when(repository.findAll()).thenReturn(query);
            when(query.page(any(Page.class))).thenReturn(query);
            when(repository.findAll(any(Sort.class))).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(new com.github.b3kt.infrastructure.persistence.entity.PermissionEntity()));

            var result = permissionService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }
    }
}
