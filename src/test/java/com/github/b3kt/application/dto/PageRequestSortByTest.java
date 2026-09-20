package com.github.b3kt.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageRequest sortBy sanitising")
class PageRequestSortByTest {

    private String sorted(String value) {
        PageRequest request = new PageRequest(1, 10);
        request.setSortBy(value);
        return request.getSortBy();
    }

    @Test
    @DisplayName("Keeps plain field references")
    void keepsFieldNames() {
        assertEquals("namaKaryawan", sorted("namaKaryawan"));
        assertEquals("tb_spk.no_spk", sorted("tb_spk.no_spk"));
        assertEquals("_internal", sorted("_internal"));
    }

    @Test
    @DisplayName("Drops anything that is not a field reference")
    void dropsInjectionAttempts() {
        assertNull(sorted("id; drop table users"));
        assertNull(sorted("(select 1)"));
        assertNull(sorted("id desc, (case when 1=1 then 1 else 2 end)"));
        assertNull(sorted("id'"));
        assertNull(sorted("id "));
        assertNull(sorted(""));
        assertNull(sorted("a".repeat(200)));
        assertNull(sorted(null));
    }
}
