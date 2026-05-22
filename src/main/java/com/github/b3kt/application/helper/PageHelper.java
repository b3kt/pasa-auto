package com.github.b3kt.application.helper;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

import java.util.List;
import java.util.Map;

public class PageHelper {

    public static <T> PageResponse<T> paginate(PanacheRepositoryBase<T, ?> repo, PageRequest pr,
                                                String queryString, Object... params) {
        PanacheQuery<T> query = params != null && params.length > 0
                ? repo.find(queryString, params)
                : repo.find(queryString);
        return applyPagination(query, repo, pr, queryString, params);
    }

    public static <T> PageResponse<T> paginate(PanacheRepositoryBase<T, ?> repo, PageRequest pr,
                                                String queryString, Parameters params) {
        PanacheQuery<T> query = repo.find(queryString, params);
        return applyPagination(query, repo, pr, queryString, params);
    }

    public static <T> PageResponse<T> paginate(PanacheRepositoryBase<T, ?> repo, PageRequest pr,
                                                String queryString, Map<String, Object> params) {
        PanacheQuery<T> query = repo.find(queryString, params);
        return applyPagination(query, repo, pr, queryString, params);
    }

    @SuppressWarnings("unchecked")
    public static <T> PageResponse<T> applyPagination(PanacheQuery<T> query, PanacheRepositoryBase<T, ?> repo,
                                                       PageRequest pr, String queryString, Object params) {
        if (pr.getSortBy() != null && !pr.getSortBy().isEmpty()) {
            Sort sort = pr.isDescending() ? Sort.descending(pr.getSortBy()) : Sort.ascending(pr.getSortBy());
            if (params instanceof Parameters p) {
                query = repo.find(queryString, sort, p);
            } else if (params instanceof Map) {
                query = repo.find(queryString, sort, (Map<String, Object>) params);
            } else if (params instanceof Object[] arr && arr.length > 0) {
                query = repo.find(queryString, sort, arr);
            } else {
                query = repo.find(queryString, sort);
            }
        }
        long totalCount = query.count();
        List<T> rows = query.page(Page.of(pr.getPage() - 1, pr.getRowsPerPage())).list();
        return new PageResponse<>(rows, pr.getPage(), pr.getRowsPerPage(), totalCount);
    }

    public static <T> PageResponse<T> findAll(PanacheRepositoryBase<T, ?> repo, PageRequest pr) {
        PanacheQuery<T> query = repo.findAll();
        if (pr.getSortBy() != null && !pr.getSortBy().isEmpty()) {
            Sort sort = pr.isDescending() ? Sort.descending(pr.getSortBy()) : Sort.ascending(pr.getSortBy());
            query = repo.findAll(sort);
        }
        long totalCount = query.count();
        List<T> rows = query.page(Page.of(pr.getPage() - 1, pr.getRowsPerPage())).list();
        return new PageResponse<>(rows, pr.getPage(), pr.getRowsPerPage(), totalCount);
    }
}
