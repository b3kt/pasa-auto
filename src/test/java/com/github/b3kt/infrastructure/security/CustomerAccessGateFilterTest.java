package com.github.b3kt.infrastructure.security;

import com.github.b3kt.application.service.AppSettingService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerAccessGateFilter Tests")
class CustomerAccessGateFilterTest {

    @Mock
    AppSettingService appSettingService;

    @Mock
    ContainerRequestContext ctx;

    @Mock
    UriInfo uriInfo;

    @InjectMocks
    CustomerAccessGateFilter filter;

    private void path(String path) {
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn(path);
    }

    private void enabled(boolean value) {
        when(appSettingService.getBooleanConfig(
                CustomerAccessGateFilter.CUSTOMER_ACCESS_ENABLED_KEY, false)).thenReturn(value);
    }

    @Nested
    @DisplayName("when the portal is disabled")
    class DisabledTests {

        @Test
        @DisplayName("aborts customer requests with a 503")
        void aborts() {
            path("api/customer/bookings");
            enabled(false);

            filter.filter(ctx);

            ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
            verify(ctx).abortWith(captor.capture());
            assertEquals(503, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("normalizes a leading slash before matching")
        void leadingSlash() {
            path("/api/customer/bookings");
            enabled(false);

            filter.filter(ctx);

            verify(ctx).abortWith(any(Response.class));
        }
    }

    @Nested
    @DisplayName("when the portal is enabled")
    class EnabledTests {

        @Test
        @DisplayName("lets customer requests through")
        void allows() {
            path("api/customer/bookings");
            enabled(true);

            filter.filter(ctx);

            verify(ctx, never()).abortWith(any());
        }
    }

    @Nested
    @DisplayName("non-customer paths")
    class OtherPathsTests {

        @Test
        @DisplayName("are never gated and never touch the setting")
        void ignored() {
            path("api/spk");

            filter.filter(ctx);

            verify(ctx, never()).abortWith(any());
            verify(appSettingService, never()).getBooleanConfig(anyString(), anyBoolean());
        }

        @Test
        @DisplayName("a path merely prefixed with the word customer is not gated")
        void similarPrefix() {
            path("api/customers");

            filter.filter(ctx);

            verify(ctx, never()).abortWith(any());
        }
    }
}
