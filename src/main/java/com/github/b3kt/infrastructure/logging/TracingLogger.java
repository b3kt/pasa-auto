package com.github.b3kt.infrastructure.logging;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for enhanced logging with traceId and spanId support
 */
@Slf4j
@ApplicationScoped
public class TracingLogger {

    @Inject
    Tracer tracer;

    /**
     * Log request information with tracing context
     */
    public void logRequest(String method, String path, Object requestBody) {
        Span span = tracer.spanBuilder(String.format("%s %s", method, path))
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", method);
            span.setAttribute("http.path", path);
            span.setAttribute("http.request.body", requestBody != null ? requestBody.toString() : "");
            
            log.debug("REQUEST: {} {} - Body: {}", method, path, requestBody);
        } finally {
            span.end();
        }
    }

    /**
     * Log response information with tracing context
     */
    public void logResponse(String method, String path, int statusCode, Object responseBody) {
        Span span = tracer.spanBuilder(String.format("%s %s Response", method, path))
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", method);
            span.setAttribute("http.path", path);
            span.setAttribute("http.status_code", statusCode);
            span.setAttribute("http.response.body", responseBody != null ? responseBody.toString() : "");
            
            log.debug("RESPONSE: {} {} - Status: {} - Body: {}", method, path, statusCode, responseBody);
        } finally {
            span.end();
        }
    }

    /**
     * Log error information with tracing context
     */
    public void logError(String method, String path, Exception exception) {
        Span span = tracer.spanBuilder(String.format("%s %s Error", method, path))
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", method);
            span.setAttribute("http.path", path);
            span.recordException(exception);
            
            log.error("ERROR: {} {} - Exception: {}", method, path, exception.getMessage(), exception);
        } finally {
            span.end();
        }
    }

    /**
     * Log general information with tracing context
     */
    public void logInfo(String message, Object... args) {
        log.info(message, args);
    }

    /**
     * Log debug information with tracing context
     */
    public void logDebug(String message, Object... args) {
        log.debug(message, args);
    }

    /**
     * Log warning information with tracing context
     */
    public void logWarning(String message, Object... args) {
        log.warn(message, args);
    }

    /**
     * Log error information with tracing context
     */
    public void logError(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    /**
     * Get current trace ID and span ID for logging
     */
    public String getTraceContext() {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            String traceId = currentSpan.getSpanContext().getTraceId();
            String spanId = currentSpan.getSpanContext().getSpanId();
            return String.format("%s,%s", traceId, spanId);
        }
        return "-,-";
    }
}
