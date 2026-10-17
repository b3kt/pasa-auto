package com.github.b3kt.presentation.exception;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RestErrorLoggerTest {

    @Test
    @DisplayName("logs 4xx and 5xx outside a request context without failing")
    void logsOutsideRequest() {
        IllegalArgumentException error = new IllegalArgumentException("boom");

        assertDoesNotThrow(() -> RestErrorLogger.log(Response.Status.BAD_REQUEST, error));
        assertDoesNotThrow(() -> RestErrorLogger.log(Response.Status.INTERNAL_SERVER_ERROR, error, "errorId=x"));
        assertDoesNotThrow(() -> RestErrorLogger.log(Response.Status.fromStatusCode(429), error));
    }
}
