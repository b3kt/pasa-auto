package com.github.b3kt.application.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryFilterBuilderTest {

    @Test
    @DisplayName("Default builder starts with 1=1")
    void defaultBuilder() {
        QueryFilterBuilder b = QueryFilterBuilder.create();
        assertEquals("1=1", b.getQueryString());
        assertEquals(0, b.getParams().length);
    }

    @Test
    @DisplayName("withSearch adds search clause for given fields")
    void withSearchSpecificFields() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withSearch("test", "field1", "field2");
        String q = b.getQueryString();
        assertTrue(q.contains("lower(field1) like ?1"));
        assertTrue(q.contains("lower(field2) like ?1"));
        assertEquals(1, b.getParams().length);
        assertEquals("%test%", b.getParams()[0]);
    }

    @Test
    @DisplayName("withSearch ignores null search")
    void withSearchNull() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withSearch(null, "field1");
        assertEquals("1=1", b.getQueryString());
        assertEquals(0, b.getParams().length);
    }

    @Test
    @DisplayName("withSearch ignores empty search")
    void withSearchEmpty() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withSearch("", "field1");
        assertEquals("1=1", b.getQueryString());
        assertEquals(0, b.getParams().length);
    }

    @Test
    @DisplayName("withSearch (single param) adds SPK search clause")
    void withSearchDefault() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withSearch("keyword");
        String q = b.getQueryString();
        assertTrue(q.contains("lower(noSpk) like ?1"));
        assertTrue(q.contains("lower(nopol) like ?1"));
        assertTrue(q.contains("lower(namaKaryawan) like ?1"));
        assertTrue(q.contains("lower(namaPelanggan) like ?1"));
        assertEquals(1, b.getParams().length);
        assertEquals("%keyword%", b.getParams()[0]);
    }

    @Test
    @DisplayName("withStatusFilter adds status IN clause")
    void withStatusFilter() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withStatusFilter("ACTIVE,PENDING", "status");
        String q = b.getQueryString();
        assertTrue(q.contains("status = ?1"));
        assertTrue(q.contains("status = ?2"));
        assertEquals(2, b.getParams().length);
        assertEquals("ACTIVE", b.getParams()[0]);
        assertEquals("PENDING", b.getParams()[1]);
    }

    @Test
    @DisplayName("withStatusFilter ignores null")
    void withStatusFilterNull() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withStatusFilter(null, "status");
        assertEquals("1=1", b.getQueryString());
    }

    @Test
    @DisplayName("withStatusFilter ignores empty")
    void withStatusFilterEmpty() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withStatusFilter("", "status");
        assertEquals("1=1", b.getQueryString());
    }

    @Test
    @DisplayName("withDateRange adds start date and/or end date")
    void withDateRange() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withDateRange("2024-01-01", "2024-01-31", "createdAt");
        String q = b.getQueryString();
        assertTrue(q.contains("createdAt >= ?1"));
        assertTrue(q.contains("createdAt < ?2"));
        assertEquals("2024-01-01", b.getParams()[0]);
        assertEquals("2024-02-01", b.getParams()[1]); // plus one day
    }

    @Test
    @DisplayName("withDateRange with only start date")
    void withDateRangeStartOnly() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withDateRange("2024-06-01", null, "createdAt");
        String q = b.getQueryString();
        assertTrue(q.contains("createdAt >= ?1"));
        assertFalse(q.contains("createdAt <"));
        assertEquals(1, b.getParams().length);
    }

    @Test
    @DisplayName("withDateRange with only end date")
    void withDateRangeEndOnly() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withDateRange(null, "2024-12-31", "createdAt");
        String q = b.getQueryString();
        assertFalse(q.contains("createdAt >="));
        assertTrue(q.contains("createdAt < ?1"));
        assertEquals(1, b.getParams().length);
    }

    @Test
    @DisplayName("Chaining multiple filters works correctly")
    void chaining() {
        QueryFilterBuilder b = QueryFilterBuilder.create()
                .withSearch("budi", "nama")
                .withStatusFilter("HADIR", "status")
                .withDateRange("2024-01-01", "2024-01-31", "tanggal");
        String q = b.getQueryString();
        assertTrue(q.contains("lower(nama) like ?1"));
        assertTrue(q.contains("status = ?2"));
        assertTrue(q.contains("tanggal >= ?3"));
        assertTrue(q.contains("tanggal < ?4"));
        assertEquals(4, b.getParams().length);
    }

    @Test
    @DisplayName("create() factory method returns new instance")
    void factoryMethod() {
        assertNotNull(QueryFilterBuilder.create());
    }
}
