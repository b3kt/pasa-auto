package com.github.b3kt.infrastructure.interceptor;

import com.github.b3kt.infrastructure.logging.TracingLogger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Priority;
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

/**
 * JAX-RS filter for logging HTTP requests and responses with traceId and spanId
 */
@Slf4j
@Provider
@Priority(Priorities.USER)
public class LoggingInterceptor implements ContainerRequestFilter, ContainerResponseFilter {

    @Inject
    TracingLogger tracingLogger;

    @Inject
    Tracer tracer;

    private static final String REQUEST_BODY_PROPERTY = "request.body";
    private static final String START_TIME_PROPERTY = "start.time";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Record start time
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());
        
        // Get request body for logging (only for certain content types and methods)
        String requestBody = getRequestBody(requestContext);
        requestContext.setProperty(REQUEST_BODY_PROPERTY, requestBody);
        
        // Create span for the request
        Span span = tracer.spanBuilder(String.format("%s %s", 
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
            tracingLogger.logRequest(
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                requestBody
            );
            
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
        
        // Get response body for logging (only for certain content types)
        String responseBody = getResponseBody(responseContext);
        
        // Create span for the response
        Span span = tracer.spanBuilder(String.format("%s %s Response", 
                requestContext.getMethod(), 
                requestContext.getUriInfo().getPath()))
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", requestContext.getMethod());
            span.setAttribute("http.path", requestContext.getUriInfo().getPath());
            span.setAttribute("http.status_code", responseContext.getStatus());
            span.setAttribute("http.response_time_ms", duration);
            
            // Log response
            tracingLogger.logResponse(
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                responseContext.getStatus(),
                responseBody
            );
            
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
                
                // Limit the size of logged request body
                if (requestBody.length() > 1000) {
                    requestBody = requestBody.substring(0, 1000) + "... [truncated]";
                }
                
                return requestBody;
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
                
                String responseBody = entity.toString();
                
                // Limit the size of logged response body
                if (responseBody.length() > 1000) {
                    responseBody = responseBody.substring(0, 1000) + "... [truncated]";
                }
                
                return responseBody;
            }
        } catch (Exception e) {
            log.warn("Failed to read response body for logging", e);
        }
        
        return "";
    }
}
