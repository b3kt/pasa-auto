package com.github.b3kt.infrastructure.web;

import com.github.b3kt.application.properties.CustomerPortalProperties;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class CustomerPortalRedirect {

    @Inject
    CustomerPortalProperties customerPortalProperties;

    void init(@Observes Router router) {
        router.get("/app").order(0).handler(ctx -> {
            if (!customerPortalProperties.enabled()) {
                ctx.response().setStatusCode(404).end();
                return;
            }
            if ("/app".equals(ctx.normalizedPath())) {
                ctx.response().setStatusCode(302).putHeader("Location", "/app/").end();
            } else {
                ctx.next();
            }
        });

        router.route("/app/*").order(0).handler(ctx -> {
            if (!customerPortalProperties.enabled()) {
                ctx.response().setStatusCode(404).end();
                return;
            }
            ctx.next();
        });
    }
}
