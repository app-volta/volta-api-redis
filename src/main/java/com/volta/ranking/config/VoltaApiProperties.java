package com.volta.ranking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "volta.api")
public record VoltaApiProperties(
        String baseUrl,
        int timeoutMillis
) {
}
