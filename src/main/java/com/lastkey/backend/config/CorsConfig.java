package com.lastkey.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // 1. Existing allowed origins copy karo
        List<String> allowedPatterns = new ArrayList<>();
        if (corsProperties.getAllowedOrigins() != null) {
            allowedPatterns.addAll(corsProperties.getAllowedOrigins());
        }

        // 2. Vercel wildcard patterns add karo taaki har Vercel build/preview URL auto-allow ho jaye
        allowedPatterns.add("https://*.vercel.app");
        allowedPatterns.add("http://localhost:*");

        // 3. setAllowedOrigins ki jagah setAllowedOriginPatterns call karo
        configuration.setAllowedOriginPatterns(allowedPatterns);

        configuration.setAllowedMethods(
                corsProperties.getAllowedMethods()
        );

        configuration.setAllowedHeaders(
                corsProperties.getAllowedHeaders()
        );

        configuration.setExposedHeaders(
                corsProperties.getExposedHeaders()
        );

        configuration.setAllowCredentials(
                corsProperties.isAllowCredentials()
        );

        configuration.setMaxAge(
                corsProperties.getMaxAge()
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}