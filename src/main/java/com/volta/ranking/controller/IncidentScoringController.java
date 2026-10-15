package com.volta.ranking.controller;

import com.volta.ranking.controller.docs.IncidentScoringControllerDocs;
import com.volta.ranking.dto.scoring.IncidentCooperativeScoresResponseDTO;
import com.volta.ranking.usecase.CooperativeScoringUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/incidents")
public class IncidentScoringController implements IncidentScoringControllerDocs {

    private final CooperativeScoringUseCase cooperativeScoringUseCase;

    @GetMapping("/{incidentId}/cooperative-scores")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OPERATOR')")
    public IncidentCooperativeScoresResponseDTO scoreCooperatives(
            @PathVariable UUID incidentId,
            @RequestParam(required = false) BigDecimal radiusKm,
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        IncidentCooperativeScoresResponseDTO scores =
                cooperativeScoringUseCase.scoreCooperatives(incidentId, radiusKm, limit, authorization);
        return scores;
    }
}
