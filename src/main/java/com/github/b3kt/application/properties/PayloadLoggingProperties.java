package com.github.b3kt.application.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;
import java.util.Optional;

/**
 * Logging of REST request and response payloads (bodies).
 *
 * <p>Off by default: payloads can hold customer data. Credentials are always masked and
 * {@code /api/auth/*} bodies are never logged, whatever these settings say.
 */
@ConfigMapping(prefix = "app.logging.payload")
public interface PayloadLoggingProperties {

    /**
     * Log request and response bodies at INFO, independent of the application log level.
     * @return true if payloads are logged
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Longest body written to the log, in characters; longer bodies are truncated.
     * Zero or negative logs the whole body.
     */
    @WithDefault("1000")
    int maxLength();

    /**
     * Path prefixes whose bodies are not logged, in addition to {@code /api/auth}
     * (e.g. {@code /api/upload,/api/pazaauto/laporan}).
     */
    Optional<List<String>> excludedPaths();
}
