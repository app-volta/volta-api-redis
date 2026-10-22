package com.volta.ranking.service;

import com.volta.ranking.client.VoltaApiClient;
import com.volta.ranking.config.ScoringProperties;
import com.volta.ranking.dto.scoring.CooperativeScoreResponseDTO;
import com.volta.ranking.dto.scoring.IncidentCooperativeScoresResponseDTO;
import com.volta.ranking.dto.voltaapi.CooperativeDataDTO;
import com.volta.ranking.dto.voltaapi.CoordinatesDTO;
import com.volta.ranking.dto.voltaapi.IncidentContextDTO;
import com.volta.ranking.exception.BusinessRuleException;
import com.volta.ranking.exception.ExternalServiceException;
import com.volta.ranking.exception.InvalidRequestException;
import com.volta.ranking.usecase.CooperativeScoringUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CooperativeScoringService implements CooperativeScoringUseCase {

    private final VoltaApiClient voltaApiClient;
    private final CooperativeScoreCalculator cooperativeScoreCalculator;
    private final ScoringProperties scoringProperties;

    public IncidentCooperativeScoresResponseDTO scoreCooperatives(
            UUID incidentId,
            BigDecimal radiusKm,
            int limit,
            String authorization
    ) {
        BigDecimal radius = radiusKm;

        if (radius == null) {
            radius = scoringProperties.defaultRadiusKm();
        }
        if (radius.signum() <= 0 || radius.compareTo(new BigDecimal("20000")) > 0) {
            throw new InvalidRequestException("Radius must be greater than 0 and at most 20000");
        }
        if (limit < 1 || limit > 100) {
            throw new InvalidRequestException("Limit must be between 1 and 100");
        }

        IncidentContextDTO incident = voltaApiClient.getIncidentContext(incidentId, authorization);
        validateIncident(incident);

        List<CooperativeDataDTO> cooperatives = voltaApiClient.getCooperatives(authorization);
        List<CooperativeScoreResponseDTO> scores =
                cooperativeScoreCalculator.score(incident, cooperatives, radius.doubleValue());

        return new IncidentCooperativeScoresResponseDTO(
                incidentId,
                incident.priority(),
                incident.wasteType().category(),
                cooperativeScoreCalculator.isHighRisk(incident),
                radius.doubleValue(),
                firstOf(scores, limit)
        );
    }

    private void validateIncident(IncidentContextDTO incident) {
        if (
                incident.priority() == null
                || incident.wasteType() == null
                || incident.wasteType().category() == null
                || incident.wasteType().defaultRiskLevel() == null
        ) {
            throw new ExternalServiceException("Volta API incident context", "priority or waste type is missing");
        }

        CoordinatesDTO origin = incident.origin();

        if (origin == null || origin.latitude() == null || origin.longitude() == null) {
            throw new BusinessRuleException("Company coordinates are not registered");
        }

        if ("RADIOATIVO".equals(incident.wasteType().category())) {
            throw new BusinessRuleException("Radioactive waste cannot be sent to cooperatives");
        }
    }

    private List<CooperativeScoreResponseDTO> firstOf(List<CooperativeScoreResponseDTO> scores, int limit) {
        List<CooperativeScoreResponseDTO> selected = new ArrayList<>();

        for (CooperativeScoreResponseDTO score : scores) {
            if (selected.size() == limit) {
                break;
            }
            selected.add(score);
        }

        return selected;
    }
}
