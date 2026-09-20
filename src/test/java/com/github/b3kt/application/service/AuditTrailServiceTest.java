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
import org.mockito.ArgumentCaptor;
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
        when(repository.findByTableName("users", 50)).thenReturn(List.of(testEntity));
        List<AuditTrailEntity> result = auditTrailService.findByTableName("users", 50);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByUserId delegates to repository")
    void findByUserId() {
        when(repository.findByUserId(1L, 50)).thenReturn(List.of(testEntity));
        List<AuditTrailEntity> result = auditTrailService.findByUserId(1L, 50);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByRecordId delegates to repository")
    void findByRecordId() {
        when(repository.findByRecordId(1L, 50)).thenReturn(List.of(testEntity));
        List<AuditTrailEntity> result = auditTrailService.findByRecordId(1L, 50);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByTableNameAndRecordId delegates to repository")
    void findByTableNameAndRecordId() {
        when(repository.findByTableNameAndRecordId("users", 1L, 50)).thenReturn(List.of(testEntity));
        List<AuditTrailEntity> result = auditTrailService.findByTableNameAndRecordId("users", 1L, 50);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findRecent returns the newest audit trails up to the limit")
    void findRecent() {
        when(repository.listRecent(200)).thenReturn(List.of(testEntity));
        assertEquals(1, auditTrailService.findRecent(200).size());
    }

    @Test
    @DisplayName("findDistinctUsernames delegates to repository")
    void findDistinctUsernames() {
        when(repository.findDistinctUsernames()).thenReturn(List.of("admin", "owner"));
        assertEquals(List.of("admin", "owner"), auditTrailService.findDistinctUsernames());
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
    @DisplayName("findPaginated with ascending sort")
    void findPaginatedWithAscendingSort() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("tableName");
        pr.setDescending(false);
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

    /** An empty search is not a filter; the unfiltered query must be used. */
    @Test
    @DisplayName("findPaginated treats an empty search as none")
    void findPaginatedEmptySearch() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("");
        pr.setSortBy("");
        when(repository.findAll(any(Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(2L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        assertEquals(2, auditTrailService.findPaginated(pr).getRowsNumber());
        verify(repository).findAll(any(Sort.class));
        verify(repository, never()).find(anyString(), any(Sort.class), anyString());
    }

    /** With no sort asked for, the audit trail reads newest first. */
    @Test
    @DisplayName("findPaginated defaults to newest first")
    void findPaginatedDefaultSort() {
        PageRequest pr = new PageRequest(1, 10);
        when(repository.findAll(any(Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        auditTrailService.findPaginated(pr);

        ArgumentCaptor<Sort> captor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(captor.capture());
        assertEquals("timestamp", captor.getValue().getColumns().getFirst().getName());
        assertEquals(Sort.Direction.Descending, captor.getValue().getColumns().getFirst().getDirection());
    }

    @Test
    @DisplayName("getRepository exposes the audit trail repository")
    void getRepositoryExposesRepository() {
        assertSame(repository, auditTrailService.getRepository());
    }

    /** A search combined with an explicit sort keeps both. */
    @Test
    @DisplayName("findPaginated sorts a filtered search ascending")
    void findPaginatedSearchAscendingSort() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("CREATE");
        pr.setSortBy("username");
        pr.setDescending(false);
        when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(testEntity));

        assertEquals(1, auditTrailService.findPaginated(pr).getRowsNumber());

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).find(anyString(), sortCaptor.capture(), patternCaptor.capture());
        assertEquals("username", sortCaptor.getValue().getColumns().getFirst().getName());
        assertEquals(Sort.Direction.Ascending, sortCaptor.getValue().getColumns().getFirst().getDirection());
        assertEquals("%create%", patternCaptor.getValue());
    }

    @Test
    @DisplayName("findPaginated sorts a filtered search descending")
    void findPaginatedSearchDescendingSort() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSearch("CREATE");
        pr.setSortBy("username");
        pr.setDescending(true);
        when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        auditTrailService.findPaginated(pr);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).find(anyString(), sortCaptor.capture(), anyString());
        assertEquals(Sort.Direction.Descending, sortCaptor.getValue().getColumns().getFirst().getDirection());
    }
}
