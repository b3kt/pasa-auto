package com.github.b3kt.application.helper;

/**
 * Partially hides personal data shown to a customer who has not yet proven they own a record, so they
 * can recognise it without reading someone else's details.
 */
public final class Masking {

    private Masking() {
    }

    /** {@code "Budi Santoso"} -> {@code "B**********o"}; null when blank. */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.charAt(0) + "*".repeat(trimmed.length() - 2) + trimmed.charAt(trimmed.length() - 1);
    }

    /** Keeps the last three characters: {@code "0812345"} -> {@code "****345"}; null when blank. */
    public static String maskContact(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        int visible = Math.min(3, trimmed.length());
        return "*".repeat(Math.max(0, trimmed.length() - visible)) + trimmed.substring(trimmed.length() - visible);
    }
}
