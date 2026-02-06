package com.github.b3kt.application.properties;

import io.smallrye.config.ConfigMapping;

/**
 * Configuration properties for RBAC module.
 */
@ConfigMapping(prefix = "app.rbac")
public interface RbacProperties {
    
    /**
     * Enable or disable RBAC module.
     * @return true if RBAC is enabled, false otherwise
     */
    boolean enabled();
}

