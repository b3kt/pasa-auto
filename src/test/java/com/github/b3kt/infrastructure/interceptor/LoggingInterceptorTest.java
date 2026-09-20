package com.github.b3kt.infrastructure.interceptor;

import com.github.b3kt.infrastructure.logging.TracingLogger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.inject.Instance;
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

import static org.junit.jupiter.api.Assertions.*;
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
    Instance<Tracer> tracerInstance;

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
        lenient().when(tracerInstance.isResolvable()).thenReturn(true);
        lenient().when(tracerInstance.get()).thenReturn(tracer);
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
    @DisplayName("request filter masks credentials in the logged body")
    void testRequestFilter_masksCredentials() throws IOException {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getHeaderString("Content-Type")).thenReturn("application/json");
        String json = "{\"username\":\"budi\",\"password\":\"s3cr3t\"}";
        when(requestContext.getEntityStream()).thenReturn(new ByteArrayInputStream(json.getBytes()));

        interceptor.filter(requestContext);

        String logged = "{\"username\":\"budi\",\"password\":\"***\"}";
        verify(requestContext).setProperty(eq("request.body"), eq(logged));
        verify(tracingLogger).logRequest(eq("POST"), eq("/api/test"), eq(logged));
        // The original body is still passed on to the endpoint
        org.mockito.ArgumentCaptor<InputStream> forwarded = org.mockito.ArgumentCaptor.forClass(InputStream.class);
        verify(requestContext).setEntityStream(forwarded.capture());
        org.junit.jupiter.api.Assertions.assertEquals(json, new String(forwarded.getValue().readAllBytes()));
    }

    @Test
    @DisplayName("request filter masks before truncating, so a cut-off secret is not logged")
    void testRequestFilter_masksBeforeTruncating() throws IOException {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getHeaderString("Content-Type")).thenReturn("application/json");
        String secret = "S".repeat(50);
        String json = "{\"note\":\"" + "x".repeat(980) + "\",\"password\":\"" + secret + "\"}";
        when(requestContext.getEntityStream()).thenReturn(new ByteArrayInputStream(json.getBytes()));

        interceptor.filter(requestContext);

        verify(requestContext).setProperty(eq("request.body"), argThat(s -> !((String) s).contains("SSSS")));
    }

    @Test
    @DisplayName("request filter never reads or logs auth request bodies")
    void testRequestFilter_omitsAuthBody() throws IOException {
        when(uriInfo.getPath()).thenReturn("/api/auth/login");
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getHeaderString("Content-Type")).thenReturn("application/json");

        interceptor.filter(requestContext);

        verify(requestContext, never()).getEntityStream();
        verify(requestContext).setProperty(eq("request.body"), eq("[omitted]"));
        verify(tracingLogger).logRequest(eq("POST"), eq("/api/auth/login"), eq("[omitted]"));
    }

    @Test
    @DisplayName("response filter never logs auth response bodies")
    void testResponseFilter_omitsAuthBody() throws IOException {
        when(uriInfo.getPath()).thenReturn("/api/auth/refresh");
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getProperty("start.time")).thenReturn(System.currentTimeMillis());
        when(responseContext.getStatus()).thenReturn(200);

        interceptor.filter(requestContext, responseContext);

        verify(responseContext, never()).getEntity();
        verify(tracingLogger).logResponse(eq("POST"), eq("/api/auth/refresh"), eq(200), eq("[omitted]"));
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

    /** A non-JSON or absent entity is not logged, and must not blow up the response filter. */
    @Test
    @DisplayName("response filter logs no body for a non-JSON or absent entity")
    void testResponseFilter_nonJsonOrAbsentEntity() throws IOException {
        when(responseContext.getStatus()).thenReturn(200);

        when(responseContext.getEntity()).thenReturn(null);
        interceptor.filter(requestContext, responseContext);

        when(responseContext.getEntity()).thenReturn("plain text");
        when(responseContext.getMediaType()).thenReturn(null);
        interceptor.filter(requestContext, responseContext);

        when(responseContext.getMediaType()).thenReturn(MediaType.valueOf("text/plain"));
        interceptor.filter(requestContext, responseContext);

        verify(tracingLogger, times(3)).logResponse(eq("GET"), eq("/api/test"), eq(200), eq(""));
    }

    /** Reading the entity must never turn a successful response into a failure. */
    @Test
    @DisplayName("response filter swallows a failure to read the entity")
    void testResponseFilter_entityReadFails() throws IOException {
        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getEntity()).thenThrow(new IllegalStateException("entity already consumed"));

        assertDoesNotThrow(() -> interceptor.filter(requestContext, responseContext));
        verify(tracingLogger).logResponse(eq("GET"), eq("/api/test"), eq(200), eq(""));
    }

    /** Without an OpenTelemetry Tracer bean the interceptor falls back to a no-op tracer. */
    @Test
    @DisplayName("an unresolvable tracer falls back to the no-op tracer")
    void testUnresolvableTracer() throws IOException {
        when(tracerInstance.isResolvable()).thenReturn(false);

        assertDoesNotThrow(() -> interceptor.filter(requestContext));
        verify(tracerInstance, never()).get();
    }
}
