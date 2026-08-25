package com.github.b3kt.infrastructure.interceptor;

import com.github.b3kt.infrastructure.logging.TracingLogger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoggingInterceptor Tests")
class LoggingInterceptorTest {

    @Mock
    TracingLogger tracingLogger;

    @Mock
    Tracer tracer;

    @Mock
    ContainerRequestContext requestContext;

    @Mock
    ContainerResponseContext responseContext;

    @InjectMocks
    LoggingInterceptor interceptor;

    private Span span;
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() {
        span = mock(Span.class);
        SpanBuilder spanBuilder = mock(SpanBuilder.class);
        uriInfo = mock(UriInfo.class);

        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setParent(any(Context.class))).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.makeCurrent()).thenReturn(mock(Scope.class));
        lenient().when(requestContext.getUriInfo()).thenReturn(uriInfo);
        lenient().when(uriInfo.getPath()).thenReturn("/api/test");
        lenient().when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/api/test"));
        lenient().when(requestContext.getMethod()).thenReturn("GET");
        lenient().when(tracingLogger.getTraceContext()).thenReturn("trace-123");
    }

    @Test
    @DisplayName("request filter logs request with JSON body")
    void testRequestFilter_withJsonPost() throws IOException {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getHeaderString("Content-Type")).thenReturn("application/json");
        String json = "{\"key\":\"value\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes());
        when(requestContext.getEntityStream()).thenReturn(is);

        interceptor.filter(requestContext);

        verify(requestContext).setProperty(eq("start.time"), any());
        verify(requestContext).setProperty(eq("request.body"), eq(json));
        verify(tracingLogger).logRequest(eq("POST"), eq("/api/test"), eq(json));
        verify(span).end();
    }

    @Test
    @DisplayName("request filter handles GET (non-JSON) request")
    void testRequestFilter_getRequest() throws IOException {
        when(requestContext.getHeaderString("Content-Type")).thenReturn(null);

        interceptor.filter(requestContext);

        verify(requestContext).setProperty(eq("request.body"), eq(""));
        verify(tracingLogger).logRequest(eq("GET"), eq("/api/test"), eq(""));
    }

    @Test
    @DisplayName("request filter truncates large body")
    void testRequestFilter_truncatesLargeBody() throws IOException {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getHeaderString("Content-Type")).thenReturn("application/json");
        String largeJson = "x".repeat(1500);
        InputStream is = new ByteArrayInputStream(largeJson.getBytes());
        when(requestContext.getEntityStream()).thenReturn(is);

        interceptor.filter(requestContext);

        verify(requestContext).setProperty(eq("request.body"), argThat(s -> ((String) s).endsWith("... [truncated]")));
    }

    @Test
    @DisplayName("response filter logs response with JSON entity")
    void testResponseFilter_withJsonEntity() throws IOException {
        when(responseContext.getEntity()).thenReturn("{\"result\":\"ok\"}");
        when(responseContext.getMediaType()).thenReturn(MediaType.valueOf("application/json"));
        when(responseContext.getStatus()).thenReturn(200);

        interceptor.filter(requestContext, responseContext);

        verify(tracingLogger).logResponse(eq("GET"), eq("/api/test"), eq(200), anyString());
        verify(span).end();
    }

    @Test
    @DisplayName("response filter handles null start time")
    void testResponseFilter_nullStartTime() throws IOException {
        when(requestContext.getProperty("start.time")).thenReturn(null);
        when(responseContext.getEntity()).thenReturn(null);

        interceptor.filter(requestContext, responseContext);

        verify(span).setAttribute(eq("http.response_time_ms"), eq(0L));
    }

    @Test
    @DisplayName("response filter handles non-JSON entity")
    void testResponseFilter_nonJsonEntity() throws IOException {
        when(responseContext.getEntity()).thenReturn("plain text");
        when(responseContext.getMediaType()).thenReturn(MediaType.valueOf("text/plain"));
        when(responseContext.getStatus()).thenReturn(200);

        interceptor.filter(requestContext, responseContext);

        verify(tracingLogger).logResponse(eq("GET"), eq("/api/test"), eq(200), eq(""));
    }

    @Test
    @DisplayName("response filter truncates large response body")
    void testResponseFilter_truncatesLargeBody() throws IOException {
        String largeResponse = "x".repeat(1500);
        when(responseContext.getEntity()).thenReturn(largeResponse);
        when(responseContext.getMediaType()).thenReturn(MediaType.valueOf("application/json"));
        when(responseContext.getStatus()).thenReturn(200);

        interceptor.filter(requestContext, responseContext);

        verify(tracingLogger).logResponse(eq("GET"), eq("/api/test"), eq(200), argThat(s -> ((String) s).endsWith("... [truncated]")));
    }

    @Test
    @DisplayName("request filter handles IOException gracefully")
    void testRequestFilter_ioException() throws IOException {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getHeaderString("Content-Type")).thenReturn("application/json");
        InputStream brokenStream = mock(InputStream.class);
        when(brokenStream.read(any(byte[].class))).thenThrow(new IOException("broken"));
        when(requestContext.getEntityStream()).thenReturn(brokenStream);

        interceptor.filter(requestContext);

        verify(requestContext).setProperty(eq("request.body"), eq(""));
    }
}
