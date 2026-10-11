package com.github.b3kt.application.helper;

import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SortFieldValidator Tests")
class SortFieldValidatorTest {

    @Test
    @DisplayName("validatedField returns null for null or blank input")
    void validatedField_nullOrBlank() {
        assertNull(SortFieldValidator.validatedField(null));
        assertNull(SortFieldValidator.validatedField(""));
        assertNull(SortFieldValidator.validatedField("   "));
    }

    @ParameterizedTest
    @DisplayName("validatedField accepts plain and dotted property paths")
    @ValueSource(strings = {"id", "namaPelanggan", "pelanggan.namaPelanggan", "_x", "a1.b2.c3"})
    void validatedField_acceptsSafe(String field) {
        assertEquals(field, SortFieldValidator.validatedField(field));
    }

    @ParameterizedTest
    @DisplayName("validatedField rejects values that could inject JPQL")
    @ValueSource(strings = {
            "id asc, (select 1)",
            "id desc",
            "nama) or 1=1",
            "id; drop table users",
            "'id'",
            "id, nama",
            "1id",
            "a..b",
            "id "
    })
    void validatedField_rejectsUnsafe(String field) {
        assertThrows(IllegalArgumentException.class, () -> SortFieldValidator.validatedField(field));
    }

    @Test
    @DisplayName("sort returns null when there is no field to sort by")
    void sort_nullWhenBlank() {
        assertNull(SortFieldValidator.sort(null, false));
        assertNull(SortFieldValidator.sort("  ", true));
    }

    @Test
    @DisplayName("sort builds an ascending sort for a safe field")
    void sort_ascending() {
        Sort sort = SortFieldValidator.sort("namaPelanggan", false);

        assertNotNull(sort);
        assertEquals(Sort.Direction.Ascending, sort.getColumns().get(0).getDirection());
        assertEquals("namaPelanggan", sort.getColumns().get(0).getName());
    }

    @Test
    @DisplayName("sort builds a descending sort for a safe field")
    void sort_descending() {
        Sort sort = SortFieldValidator.sort("id", true);

        assertNotNull(sort);
        assertEquals(Sort.Direction.Descending, sort.getColumns().get(0).getDirection());
    }

    @Test
    @DisplayName("sort refuses an unsafe field")
    void sort_unsafe() {
        assertThrows(IllegalArgumentException.class, () -> SortFieldValidator.sort("id desc", false));
    }
}
