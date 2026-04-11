package com.github.b3kt.application.properties;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app.security")
public interface SecurityProperties {
    
    String salt();
}
