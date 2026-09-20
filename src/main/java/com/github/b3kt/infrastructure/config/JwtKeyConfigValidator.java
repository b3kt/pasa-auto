package com.github.b3kt.infrastructure.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Fails startup with an actionable message when the JWT keys are missing or unreadable, instead of
 * letting the application come up and reject every login later. Keys are deliberately not bundled with
 * the application, so they must be provided at runtime.
 */
@Slf4j
@ApplicationScoped
public class JwtKeyConfigValidator {

    static final String SIGN_KEY_PROPERTY = "smallrye.jwt.sign.key.location";
    static final String VERIFY_KEY_PROPERTY = "mp.jwt.verify.publickey.location";

    void onStart(@Observes StartupEvent event) {
        validate(SIGN_KEY_PROPERTY, "JWT_PRIVATE_KEY");
        validate(VERIFY_KEY_PROPERTY, "JWT_PUBLIC_KEY");
    }

    private void validate(String property, String envVariable) {
        String location = ConfigProvider.getConfig().getOptionalValue(property, String.class)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing JWT key configuration: set " + envVariable + " (property " + property + "), "
                                + "for example " + envVariable + "=file:/etc/pasa-auto/keys/key.pem"));

        if (!isReadable(location)) {
            throw new IllegalStateException(
                    "JWT key configured by " + envVariable + " cannot be read: " + location
                            + ". Keys are not packaged with the application, so this must point at a readable file "
                            + "(file:/path/to/key.pem) on the machine running it.");
        }
    }

    private boolean isReadable(String location) {
        if (location.startsWith("classpath:")) {
            String resource = location.substring("classpath:".length());
            return Thread.currentThread().getContextClassLoader().getResource(resource) != null;
        }
        if (location.startsWith("http://") || location.startsWith("https://")) {
            // A remote JWKS endpoint is fetched by the JWT extension itself; nothing to check here
            return true;
        }
        return Optional.of(location.startsWith("file:") ? location.substring("file:".length()) : location)
                .map(Path::of)
                .filter(Files::isReadable)
                .isPresent();
    }
}
