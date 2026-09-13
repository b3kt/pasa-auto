package com.github.b3kt.infrastructure.logging;

import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class TracingLogger {

    public void logRequest(String method, String path, Object requestBody) {
        log.debug("[{}] REQUEST: {} {} - Body: {}",
                getTraceContext(), method, path, requestBody);
    }

    public void logResponse(String method, String path, int statusCode, Object responseBody) {
        log.debug("[{}] RESPONSE: {} {} - Status: {} - Body: {}",
                getTraceContext(), method, path, statusCode, responseBody);
    }

    public void logError(String method, String path, Exception exception) {
        log.error("[{}] ERROR: {} {} - Exception: {}",
                getTraceContext(), method, path, exception.getMessage(), exception);
    }

    public void logInfo(String message, Object... args) {
        log.info(message, args);
    }

    public void logDebug(String message, Object... args) {
        log.debug(message, args);
    }

    public void logWarning(String message, Object... args) {
        log.warn(message, args);
    }

    public void logError(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    public String getTraceContext() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            String traceId = currentSpan.getSpanContext().getTraceId();
            String spanId = currentSpan.getSpanContext().getSpanId();
            return String.format("%s,%s", traceId, spanId);
        }
        return "-,-";
    }
}
