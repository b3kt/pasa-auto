package com.github.b3kt.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    @DisplayName("Default constructor")
    void defaultConstructor() {
        PageResponse<String> r = new PageResponse<>();
        assertNull(r.getRows());
        assertEquals(0, r.getPage());
        assertEquals(0, r.getRowsPerPage());
        assertEquals(0, r.getRowsNumber());
    }

    @Test
    @DisplayName("Parameterized constructor")
    void parameterizedConstructor() {
        List<String> rows = List.of("a", "b", "c");
        PageResponse<String> r = new PageResponse<>(rows, 1, 10, 3);
        assertEquals(rows, r.getRows());
        assertEquals(1, r.getPage());
        assertEquals(10, r.getRowsPerPage());
        assertEquals(3, r.getRowsNumber());
    }

    @Test
    @DisplayName("Setters")
    void setters() {
        PageResponse<String> r = new PageResponse<>();
        List<String> rows = List.of("x");
        r.setRows(rows);
        r.setPage(2);
        r.setRowsPerPage(5);
        r.setRowsNumber(1);
        assertEquals(rows, r.getRows());
        assertEquals(2, r.getPage());
        assertEquals(5, r.getRowsPerPage());
        assertEquals(1, r.getRowsNumber());
    }
}
