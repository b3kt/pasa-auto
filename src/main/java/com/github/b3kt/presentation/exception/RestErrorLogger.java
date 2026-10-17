package com.github.b3kt.presentation.exception;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * Logs the exception behind every non-2xx REST response, with its stack trace and the request it
 * answered ({@code "PUT /api/pazaauto/pelanggan/12 -> 400 Bad Request"}). Client errors (4xx) are
 * logged at WARN, server errors (5xx) at ERROR.
 *
 * <p>Everything goes to the dedicated category {@value #CATEGORY}, so its verbosity can be tuned on its
 * own, e.g. {@code quarkus.log.category."com.github.b3kt.rest.errors".level=ERROR} to keep only 5xx.
 * The query string is never logged (it may carry search terms or tokens).
 */
public final class RestErrorLogger {

    public static final String CATEGORY = "com.github.b3kt.rest.errors";

    private static final Logger LOG = Logger.getLogger(CATEGORY);

    private RestErrorLogger() {
    }

    public static void log(Response.StatusType status, Throwable error) {
        log(status, error, null);
    }

    /** @param detail extra context appended to the summary, e.g. the error reference returned to the client */
    public static void log(Response.StatusType status, Throwable error, String detail) {
        String summary = currentRequest() + " -> " + status.getStatusCode() + " " + status.getReasonPhrase()
                + (detail == null ? "" : " [" + detail + "]");
        if (status.getFamily() == Response.Status.Family.SERVER_ERROR) {
            LOG.error(summary, error);
        } else {
            LOG.warn(summary, error);
        }
    }

    /** {@code "METHOD /path"} of the request being handled, or {@code "request"} outside one. */
    private static String currentRequest() {
        try {
            ArcContainer container = Arc.container();
            if (container == null || !container.requestContext().isActive()) {
                return "request";
            }
            RoutingContext context = container.instance(CurrentVertxRequest.class).get().getCurrent();
            return context == null ? "request" : context.request().method() + " " + context.request().path();
        } catch (RuntimeException e) {
            return "request";
        }
    }
}
