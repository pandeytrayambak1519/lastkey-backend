package com.lastkey.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins =
            new ArrayList<>();

    private List<String> allowedMethods =
            new ArrayList<>();

    private List<String> allowedHeaders =
            new ArrayList<>();

    private List<String> exposedHeaders =
            new ArrayList<>();

    private boolean allowCredentials = true;

    private long maxAge = 3600;
}