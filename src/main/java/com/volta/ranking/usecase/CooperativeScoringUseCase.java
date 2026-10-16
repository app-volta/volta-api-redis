package com.volta.ranking.usecase;

import com.volta.ranking.dto.scoring.IncidentCooperativeScoresResponseDTO;

import java.math.BigDecimal;
import java.util.UUID;

public interface CooperativeScoringUseCase {

    IncidentCooperativeScoresResponseDTO scoreCooperatives(
            UUID incidentId,
            BigDecimal radiusKm,
            int limit,
            String authorization
    );
}
