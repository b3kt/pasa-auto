package com.github.b3kt.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageRequestTest {

    @Test
    @DisplayName("Default constructor sets sensible defaults")
    void defaultConstructor() {
        PageRequest r = new PageRequest();
        assertEquals(1, r.getPage());
        assertEquals(10, r.getRowsPerPage());
        assertFalse(r.isDescending());
        assertFalse(r.isFilterToday());
    }

    @Test
    @DisplayName("Parameterized constructor sets page and rowsPerPage")
    void parameterizedConstructor() {
        PageRequest r = new PageRequest(2, 20);
        assertEquals(2, r.getPage());
        assertEquals(20, r.getRowsPerPage());
    }

    @Test
    @DisplayName("getOffset computes correct offset")
    void getOffset() {
        PageRequest r = new PageRequest(1, 10);
        assertEquals(0, r.getOffset());

        r.setPage(3);
        r.setRowsPerPage(15);
        assertEquals(30, r.getOffset());
    }

    @Test
    @DisplayName("Setters for all filter fields")
    void setters() {
        PageRequest r = new PageRequest();
        r.setSortBy("name");
        r.setDescending(true);
        r.setSearch("keyword");
        r.setStatusFilter("ACTIVE");
        r.setFilterToday(true);
        r.setJenisPembelianFilter("SPAREPART");
        r.setKategoriOperasionalFilter("OPERASIONAL");
        r.setStartDate("2024-01-01");
        r.setEndDate("2024-01-31");
        r.setSupplierId(5L);

        assertEquals("name", r.getSortBy());
        assertTrue(r.isDescending());
        assertEquals("keyword", r.getSearch());
        assertEquals("ACTIVE", r.getStatusFilter());
        assertTrue(r.isFilterToday());
        assertEquals("SPAREPART", r.getJenisPembelianFilter());
        assertEquals("OPERASIONAL", r.getKategoriOperasionalFilter());
        assertEquals("2024-01-01", r.getStartDate());
        assertEquals("2024-01-31", r.getEndDate());
        assertEquals(5L, r.getSupplierId());
    }
}
