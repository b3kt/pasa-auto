package com.github.b3kt.application.properties;

import io.smallrye.config.ConfigMapping;

/**
 * Configuration for the customer portal feature.
 *
 * <p>Controls whether the customer portal (/app/) is accessible and whether
 * the link to it is shown on the login page.
 */
@ConfigMapping(prefix = "app.features.customer-portal")
public interface CustomerPortalProperties {

    /**
     * Enable or disable the customer portal. While false:
     * - The "Are you a customer? Sign in here" link is hidden on the login page
     * - Access to /app/ returns 404
     * @return true if the customer portal is enabled
     */
    boolean enabled();
}
