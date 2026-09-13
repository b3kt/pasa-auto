package com.github.b3kt.infrastructure.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TracingLoggerTest {

    private final TracingLogger logger = new TracingLogger();

    @Test
    @DisplayName("getTraceContext returns '-,-' when no span is active")
    void getTraceContextNoSpan() {
        String ctx = logger.getTraceContext();
        assertEquals("-,-", ctx);
    }

    @Test
    @DisplayName("log methods do not throw")
    void logMethods() {
        assertDoesNotThrow(() -> logger.logRequest("GET", "/api/test", "body"));
        assertDoesNotThrow(() -> logger.logResponse("GET", "/api/test", 200, "body"));
        assertDoesNotThrow(() -> logger.logError("GET", "/api/test", new RuntimeException("test")));
        assertDoesNotThrow(() -> logger.logInfo("info message"));
        assertDoesNotThrow(() -> logger.logDebug("debug message"));
        assertDoesNotThrow(() -> logger.logWarning("warn message"));
        assertDoesNotThrow(() -> logger.logError("error message", new RuntimeException("test")));
    }
}
