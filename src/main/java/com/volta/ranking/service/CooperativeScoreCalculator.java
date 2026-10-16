package com.volta.ranking.service;

import com.volta.ranking.config.ScoringProperties;
import com.volta.ranking.dto.scoring.CooperativeScoreResponseDTO;
import com.volta.ranking.dto.scoring.ScoreBreakdownResponseDTO;
import com.volta.ranking.dto.voltaapi.AiReportDTO;
import com.volta.ranking.dto.voltaapi.CooperativeDataDTO;
import com.volta.ranking.dto.voltaapi.CoordinatesDTO;
import com.volta.ranking.dto.voltaapi.IncidentContextDTO;
import com.volta.ranking.utils.GeoDistance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CooperativeScoreCalculator {

    private final ScoringProperties properties;

    public boolean isHighRisk(IncidentContextDTO incident) {
        AiReportDTO aiReport = incident.aiReport();

        if (aiReport != null && isHigh(aiReport.aiContaminationLevel())) {
            return true;
        }

        return isHigh(incident.contaminationLevel()) || isHigh(incident.wasteType().defaultRiskLevel());
    }

    public List<CooperativeScoreResponseDTO> score(
            IncidentContextDTO incident,
            List<CooperativeDataDTO> cooperatives,
            double radiusKm
    ) {
        String category = incident.wasteType().category();
        boolean requiresHazardousSpecialty = isHigh(incident.wasteType().defaultRiskLevel());
        boolean highRisk = isHighRisk(incident);
        ScoringProperties.Weights weights = properties.standardWeights();
        CoordinatesDTO origin = incident.origin();

        if ("HIGH".equalsIgnoreCase(incident.priority()) || "CRITICAL".equalsIgnoreCase(incident.priority())) {
            weights = properties.urgentWeights();
        }

        List<CooperativeScoreResponseDTO> scores = new ArrayList<>();

        for (CooperativeDataDTO cooperative : cooperatives) {
            List<String> specialties = cooperative.specialties();

            if (cooperative.latitude() == null || cooperative.longitude() == null) {
                continue;
            }
            if (specialties == null || !specialties.contains(category)) {
                continue;
            }

            boolean hazardousSpecialty = specialties.contains("PERIGOSO");

            if (requiresHazardousSpecialty && !hazardousSpecialty) {
                continue;
            }

            double distanceKm = GeoDistance.kilometers(
                    origin.latitude(), origin.longitude(), cooperative.latitude(), cooperative.longitude());

            if (distanceKm > radiusKm) {
                continue;
            }

            double distanceFactor = 1 - distanceKm / radiusKm;
            double ratingFactor = ratingFactor(cooperative.averageRating());
            double fitFactor = 1.0;

            if (highRisk && !hazardousSpecialty) {
                fitFactor = properties.highRiskFit();
            }

            double totalWeight = weights.distance() + weights.rating() + weights.fit();
            double weighted = weights.distance() * distanceFactor
                    + weights.rating() * ratingFactor
                    + weights.fit() * fitFactor;
            double score = 100 * weighted / totalWeight;

            ScoreBreakdownResponseDTO breakdown = new ScoreBreakdownResponseDTO(
                    round(distanceFactor * 100),
                    round(ratingFactor * 100),
                    round(fitFactor * 100));

            scores.add(new CooperativeScoreResponseDTO(
                    cooperative.id(), cooperative.name(), round(score), round(distanceKm), breakdown));
        }

        scores.sort(this::compare);
        return scores;
    }

    private boolean isHigh(String level) {
        return "HIGH".equalsIgnoreCase(level);
    }

    private double ratingFactor(BigDecimal averageRating) {
        if (averageRating == null || averageRating.signum() <= 0) {
            return properties.unratedFactor();
        }

        return Math.min(averageRating.doubleValue(), 5.0) / 5.0;
    }

    private int compare(CooperativeScoreResponseDTO first, CooperativeScoreResponseDTO second) {
        int byScore = Double.compare(second.score(), first.score());

        if (byScore != 0) {
            return byScore;
        }

        int byDistance = Double.compare(first.distanceKm(), second.distanceKm());

        if (byDistance != 0) {
            return byDistance;
        }

        String firstName = Objects.toString(first.name(), "");
        String secondName = Objects.toString(second.name(), "");
        return firstName.compareToIgnoreCase(secondName);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
