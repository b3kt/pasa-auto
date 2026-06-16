package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.AuditTrailEntity;
import com.github.b3kt.infrastructure.persistence.repository.AuditTrailRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditTrailServiceTest {

    @Mock
    AuditTrailRepository repository;

    @Mock
    PanacheQuery<AuditTrailEntity> query;

    @InjectMocks
    AuditTrailService auditTrailService;

    private AuditTrailEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new AuditTrailEntity();
        testEntity.setId(1L);
        testEntity.setTableName("users");
        testEntity.setAction("CREATE");
    }

    @Test
    @DisplayName("record persists audit trail with timestamp")
    void record() {
        doNothing().when(repository).persist(any(AuditTrailEntity.class));
        auditTrailService.record(testEntity);
        assertNotNull(testEntity.getTimestamp());
        verify(repository).persist(any(AuditTrailEntity.class));
    }

    @Test
    @DisplayName("findByTableName delegates to repository")
    void findByTableName() {
        when(repository.findByTableName("users")).thenReturn(List.of(testEntity));
        List<AuditTrailEntity> result = auditTrailService.findByTableName("users");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByUserId delegates to repository")
    void findByUserId() {
        when(repository.findByUserId(1L)).thenReturn(List.of(testEntity));
        List<AuditTrailEntity> result = auditTrailService.findByUserId(1L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByRecordId delegates to repository")
    void findByRecordId() {
        when(repository.findByRecordId(1L)).thenReturn(List.of(testEntity));
        List<AuditTrailEntity> result = auditTrailService.findByRecordId(1L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByTableNameAndRecordId delegates to repository")
    void findByTableNameAndRecordId() {
        when(repository.findByTableNameAndRecordId("users", 1L)).thenReturn(List.of(testEntity));
        List<AuditTrailEntity> result = auditTrailService.findByTableNameAndRecordId("users", 1L);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findAll returns all audit trails")
    void findAll() {
        when(repository.listAll()).thenReturn(List.of(testEntity));
        assertEquals(1, auditTrailService.findAll().size());
    }

    @Test
    @DisplayName("findPaginated with search")
    void findPaginatedWithSearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("CREATE");
        when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<AuditTrailEntity> result = auditTrailService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated without search")
    void findPaginatedNoSearch() {
        PageRequest pr = new PageRequest(1, 10);
        when(repository.findAll(any(Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<AuditTrailEntity> result = auditTrailService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated with sortBy")
    void findPaginatedWithSortBy() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("timestamp");
        when(repository.findAll(any(Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        PageResponse<AuditTrailEntity> result = auditTrailService.findPaginated(pr);
        assertEquals(1, result.getRowsNumber());
    }

    @Test
    @DisplayName("getRepository returns the repository")
    void getRepository() {
        assertEquals(repository, auditTrailService.getRepository());
    }
}
