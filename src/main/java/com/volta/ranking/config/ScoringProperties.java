package com.volta.ranking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "volta.scoring")
public record ScoringProperties(
        BigDecimal defaultRadiusKm,
        double unratedFactor,
        double highRiskFit,
        Weights urgentWeights,
        Weights standardWeights
) {

    public record Weights(
            double distance,
            double rating,
            double fit
    ) {
    }
}
