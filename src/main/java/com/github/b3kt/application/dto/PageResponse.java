package com.github.b3kt.application.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

/**
 * DTO for paginated response data.
 */
@RegisterForReflection
public class PageResponse<T> {
    private List<T> rows;
    private int page;
    private int rowsPerPage;
    private long rowsNumber;

    public PageResponse() {
    }

    public PageResponse(List<T> rows, int page, int rowsPerPage, long rowsNumber) {
        this.rows = rows;
        this.page = page;
        this.rowsPerPage = rowsPerPage;
        this.rowsNumber = rowsNumber;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }

    public long getRowsNumber() {
        return rowsNumber;
    }

    public void setRowsNumber(long rowsNumber) {
        this.rowsNumber = rowsNumber;
    }
}
