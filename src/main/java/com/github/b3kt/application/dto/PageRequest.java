package com.github.b3kt.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for pagination request parameters.
 */
@Getter
@Setter
@NoArgsConstructor
public class PageRequest {
    private int page = 1;
    private int rowsPerPage = 10;
    private String sortBy;
    private boolean descending = false;
    private String search;
    private String statusFilter;
    private boolean filterToday = false;
    private String jenisPembelianFilter;
    private String kategoriOperasionalFilter;
    private String startDate;
    private String endDate;
    private Long supplierId;

    public PageRequest(int page, int rowsPerPage) {
        this.page = page;
        this.rowsPerPage = rowsPerPage;
    }

    public int getOffset() {
        return (page - 1) * rowsPerPage;
    }
}
