package com.github.b3kt.application.properties;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app.security")
public interface SecurityProperties {    
    static String salt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'salt'");
    }
}
