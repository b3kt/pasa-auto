package com.github.b3kt.infrastructure.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks credentials (passwords, tokens, secrets) in request/response bodies before they are logged.
 */
public final class LogSanitizer {

    static final String MASK = "***";

    /** Field names are matched case-insensitively and by substring, e.g. refreshToken, newPassword, clientSecret. */
    private static final String SENSITIVE_KEY = "[A-Za-z0-9_-]*(?:password|passwd|token|secret|authorization|credential|salt)[A-Za-z0-9_-]*";

    /** JSON: "key": "value" / "key": 123 / "key": true / "key": null */
    private static final Pattern JSON_FIELD = Pattern.compile(
            "(\"(?i:" + SENSITIVE_KEY + ")\"\\s*:\\s*)(\"(?:[^\"\\\\]|\\\\.)*\"?|[^,}\\]\\s]+)");

    /** Lombok / record toString(): key=value */
    private static final Pattern TO_STRING_FIELD = Pattern.compile(
            "(\\b(?i:" + SENSITIVE_KEY + ")=)([^,)\\]}\\s]*)");

    private LogSanitizer() {
    }

    public static String mask(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        String masked = JSON_FIELD.matcher(body).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + "\"" + MASK + "\""));
        return TO_STRING_FIELD.matcher(masked).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + MASK));
    }
}
