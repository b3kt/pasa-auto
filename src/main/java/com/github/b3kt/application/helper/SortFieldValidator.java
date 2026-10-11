package com.github.b3kt.application.helper;

import io.quarkus.panache.common.Sort;

import java.util.regex.Pattern;

/**
 * Validates a client-supplied sort field before it is handed to Panache {@link Sort}.
 *
 * <p>Panache interpolates the column into the JPQL {@code ORDER BY} clause verbatim rather than
 * binding it as a parameter, so an unchecked {@code sortBy} query parameter would be a JPQL
 * injection. Only a single, dotted property path made of identifier characters is accepted;
 * whitespace, commas, quotes, parentheses, operators and the like are refused.
 */
public final class SortFieldValidator {

    private static final Pattern SAFE_FIELD =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    private SortFieldValidator() {
    }

    /**
     * @return {@code sortBy} when it is a plain property path, or {@code null} when it is null/blank
     * @throws IllegalArgumentException when the value is not a plain property path
     */
    public static String validatedField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return null;
        }
        if (!SAFE_FIELD.matcher(sortBy).matches()) {
            throw new IllegalArgumentException("Unsupported sort field");
        }
        return sortBy;
    }

    /**
     * Builds a Panache sort for {@code sortBy}, refusing unsafe values.
     *
     * @return the sort, or {@code null} when there is no field to sort by
     */
    public static Sort sort(String sortBy, boolean descending) {
        String field = validatedField(sortBy);
        if (field == null) {
            return null;
        }
        return descending ? Sort.descending(field) : Sort.ascending(field);
    }
}
