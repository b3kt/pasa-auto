package com.github.b3kt.infrastructure.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.b3kt.application.properties.PayloadLoggingProperties;
import com.github.b3kt.infrastructure.logging.LogSanitizer;
import com.github.b3kt.infrastructure.logging.TracingLogger;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JAX-RS filter for logging HTTP requests and responses with traceId and spanId.
 * Bodies are only read and logged when {@code app.logging.payload.enabled} is true.
 */
@Slf4j
@Provider
@Priority(Priorities.USER)
public class LoggingInterceptor implements ContainerRequestFilter, ContainerResponseFilter {

    @Inject
    TracingLogger tracingLogger;

    @Inject
    @Any
    Instance<Tracer> tracer;

    @Inject
    PayloadLoggingProperties payloadLogging;

    @Inject
    ObjectMapper objectMapper;

    private static final Tracer NOOP_TRACER = OpenTelemetry.noop()
            .getTracer(LoggingInterceptor.class.getName());

    private Tracer getTracer() {
        return tracer.isResolvable() ? tracer.get() : NOOP_TRACER;
    }

    private static final String REQUEST_BODY_PROPERTY = "request.body";
    private static final String START_TIME_PROPERTY = "start.time";
    private static final String OMITTED_BODY = "[omitted]";

    /** Auth requests and responses carry passwords and tokens; their bodies are never logged. */
    private static final String AUTH_PATH = "/api/auth";

    /** The auth prefix plus any configured ones; paths may come with or without a leading slash. */
    private boolean isOmittedPath(ContainerRequestContext requestContext) {
        String path = stripLeadingSlash(requestContext.getUriInfo().getPath());
        if (path == null) {
            return false;
        }
        if (path.startsWith(stripLeadingSlash(AUTH_PATH))) {
            return true;
        }
        List<String> excluded = payloadLogging.excludedPaths().orElse(List.of());
        return excluded.stream()
                .map(prefix -> stripLeadingSlash(prefix.trim()))
                .anyMatch(prefix -> !prefix.isEmpty() && path.startsWith(prefix));
    }

    private static String stripLeadingSlash(String path) {
        return path != null && path.startsWith("/") ? path.substring(1) : path;
    }

    /** Mask credentials first, then truncate, so a cut-off value can't slip past the mask. */
    private String sanitize(String body) {
        String masked = LogSanitizer.mask(body);
        int maxLength = payloadLogging.maxLength();
        if (maxLength > 0 && masked.length() > maxLength) {
            return masked.substring(0, maxLength) + "... [truncated]";
        }
        return masked;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Record start time
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());
        
        // Get request body for logging (only when enabled, for certain content types and methods)
        boolean logPayload = payloadLogging.enabled();
        String requestBody = null;
        if (logPayload) {
            requestBody = getRequestBody(requestContext);
            requestContext.setProperty(REQUEST_BODY_PROPERTY, requestBody);
        }
        
        // Create span for the request
        Span span = getTracer().spanBuilder(String.format("%s %s", 
                requestContext.getMethod(), 
                requestContext.getUriInfo().getPath()))
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", requestContext.getMethod());
            span.setAttribute("http.path", requestContext.getUriInfo().getPath());
            span.setAttribute("http.url", requestContext.getUriInfo().getRequestUri().toString());
            span.setAttribute("http.user_agent", requestContext.getHeaderString("User-Agent"));
            span.setAttribute("http.remote_addr", requestContext.getUriInfo().getRequestUri().getHost());
            
            // Log request
            if (logPayload) {
                tracingLogger.logRequest(
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getPath(),
                    requestBody
                );
            }
            
            log.info("[{}] Req: {} {}",
                tracingLogger.getTraceContext(), 
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath()
            );
        } finally {
            span.end();
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // Calculate duration
        Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        // Get response body for logging (only when enabled, for certain content types)
        boolean logPayload = payloadLogging.enabled();
        String responseBody = null;
        if (logPayload) {
            responseBody = isOmittedPath(requestContext) ? OMITTED_BODY : getResponseBody(responseContext);
        }
        
        // Create span for the response
        Span span = getTracer().spanBuilder(String.format("%s %s Response", 
                requestContext.getMethod(), 
                requestContext.getUriInfo().getPath()))
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", requestContext.getMethod());
            span.setAttribute("http.path", requestContext.getUriInfo().getPath());
            span.setAttribute("http.status_code", responseContext.getStatus());
            span.setAttribute("http.response_time_ms", duration);
            
            // Log response
            if (logPayload) {
                tracingLogger.logResponse(
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getPath(),
                    responseContext.getStatus(),
                    responseBody
                );
            }
            
            log.info("[{}] Res: {} {} | {} | {}ms", 
                tracingLogger.getTraceContext(),    
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                responseContext.getStatus(),
                duration
            );
        } finally {
            span.end();
        }
    }

    private String getRequestBody(ContainerRequestContext requestContext) {
        try {
            // Only log request body for specific content types and methods
            String contentType = requestContext.getHeaderString("Content-Type");
            String method = requestContext.getMethod();
            
            if ((contentType != null && contentType.contains("application/json")) && 
                ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
                if (isOmittedPath(requestContext)) {
                    return OMITTED_BODY;
                }
                
                InputStream inputStream = requestContext.getEntityStream();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                
                String requestBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                
                // Reset the input stream so it can be read again
                requestContext.setEntityStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
                
                return sanitize(requestBody);
            }
        } catch (IOException e) {
            log.warn("Failed to read request body for logging", e);
        }
        
        return "";
    }

    private String getResponseBody(ContainerResponseContext responseContext) {
        try {
            // Only log response body for JSON content types
            Object entity = responseContext.getEntity();
            if (entity != null && responseContext.getMediaType() != null && 
                responseContext.getMediaType().toString().contains("application/json")) {
                
                return sanitize(toJson(entity));
            }
        } catch (Exception e) {
            log.warn("Failed to read response body for logging", e);
        }
        
        return "";
    }

    /** The entity as the client receives it; toString() is only a fallback for unserializable entities. */
    private String toJson(Object entity) {
        if (entity instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            log.debug("Failed to serialize response body for logging, using toString()", e);
            return entity.toString();
        }
    }
}
