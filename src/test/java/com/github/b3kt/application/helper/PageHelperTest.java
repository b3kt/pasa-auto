package com.github.b3kt.application.helper;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageHelperTest {

    @Mock
    PanacheRepositoryBase<Object, ?> repo;

    @Mock
    PanacheQuery<Object> query;

    @Test
    @DisplayName("paginate with varargs params calls find with query string and params")
    void paginateWithVarargs() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("name");
        pr.setDescending(false);
        when(repo.find(anyString(), any(Object[].class))).thenReturn(query);
        when(repo.find(anyString(), any(Sort.class), any(Object[].class))).thenReturn(query);
        when(query.count()).thenReturn(5L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(new Object()));

        PageResponse<Object> result = PageHelper.paginate(repo, pr, "1=1", "param1");

        assertNotNull(result);
        assertEquals(1, result.getPage());
        assertEquals(10, result.getRowsPerPage());
        assertEquals(5, result.getRowsNumber());
        assertEquals(1, result.getRows().size());
        verify(repo).find(anyString(), any(Sort.class), any(Object[].class));
    }

    @Test
    @DisplayName("paginate with empty params calls find with query string only")
    void paginateWithEmptyParams() {
        PageRequest pr = new PageRequest(1, 10);
        when(repo.find(anyString())).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.paginate(repo, pr, "1=1");

        verify(repo).find(anyString());
    }

    @Test
    @DisplayName("paginate with Parameters object")
    void paginateWithParameters() {
        PageRequest pr = new PageRequest(1, 10);
        Parameters params = Parameters.with("key", "value");
        when(repo.find(anyString(), any(Parameters.class))).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.paginate(repo, pr, "1=1", params);

        verify(repo).find(anyString(), any(Parameters.class));
    }

    @Test
    @DisplayName("paginate with Map params")
    void paginateWithMap() {
        PageRequest pr = new PageRequest(1, 10);
        Map<String, Object> params = Map.of("key", "value");
        when(repo.find(anyString(), any(Map.class))).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.paginate(repo, pr, "1=1", params);

        verify(repo).find(anyString(), any(Map.class));
    }

    @Test
    @DisplayName("applyPagination with descending sort")
    void applyPaginationDescending() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("name");
        pr.setDescending(true);
        when(repo.find(anyString(), any(Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.applyPagination(query, repo, pr, "1=1", null);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repo).find(anyString(), sortCaptor.capture());
        assertEquals(Sort.Direction.Descending, sortCaptor.getValue().getColumns().get(0).getDirection());
    }

    @Test
    @DisplayName("applyPagination with ascending sort")
    void applyPaginationAscending() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("name");
        pr.setDescending(false);
        when(repo.find(anyString(), any(Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.applyPagination(query, repo, pr, "1=1", null);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repo).find(anyString(), sortCaptor.capture());
        assertEquals(Sort.Direction.Ascending, sortCaptor.getValue().getColumns().get(0).getDirection());
    }

    @Test
    @DisplayName("applyPagination with Parameters sort")
    void applyPaginationWithParametersSort() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("name");
        when(repo.find(anyString(), any(Sort.class), any(Parameters.class))).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.applyPagination(query, repo, pr, "1=1", Parameters.with("k", "v"));

        verify(repo).find(anyString(), any(Sort.class), any(Parameters.class));
    }

    @Test
    @DisplayName("applyPagination with Map sort")
    void applyPaginationWithMapSort() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("name");
        when(repo.find(anyString(), any(Sort.class), any(Map.class))).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.applyPagination(query, repo, pr, "1=1", Map.of("k", "v"));

        verify(repo).find(anyString(), any(Sort.class), any(Map.class));
    }

    @Test
    @DisplayName("applyPagination with array params sort")
    void applyPaginationWithArraySort() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("name");
        when(repo.find(anyString(), any(Sort.class), any(Object[].class))).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.applyPagination(query, repo, pr, "1=1", new Object[]{"val"});

        verify(repo).find(anyString(), any(Sort.class), any(Object[].class));
    }

    @Test
    @DisplayName("findAll without sort")
    void findAllNoSort() {
        PageRequest pr = new PageRequest(1, 10);
        when(repo.findAll()).thenReturn(query);
        when(query.count()).thenReturn(2L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(new Object(), new Object()));

        PageResponse<Object> result = PageHelper.findAll(repo, pr);

        assertEquals(2, result.getRowsNumber());
    }

    @Test
    @DisplayName("findAll with sort")
    void findAllWithSort() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setSortBy("name");
        pr.setDescending(true);
        when(repo.findAll()).thenReturn(query);
        when(repo.findAll(any(Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.findAll(repo, pr);

        verify(repo).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("paginate with null params array")
    void paginateNullParams() {
        PageRequest pr = new PageRequest(1, 10);
        when(repo.find(anyString())).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        PageHelper.paginate(repo, pr, "1=1", (Object[]) null);

        verify(repo).find(anyString());
    }

    @Test
    @DisplayName("applyPagination without sort uses original query")
    void applyPaginationNoSort() {
        PageRequest pr = new PageRequest(1, 10);
        when(query.count()).thenReturn(1L);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(new Object()));

        PageResponse<Object> result = PageHelper.applyPagination(query, repo, pr, "1=1", new Object[0]);

        assertEquals(1, result.getRowsNumber());
        verify(repo, never()).find(anyString(), any(Sort.class), any(Object[].class));
    }
}
