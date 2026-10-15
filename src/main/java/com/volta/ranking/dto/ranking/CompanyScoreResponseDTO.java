package com.volta.ranking.dto.ranking;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Pontuação de uma empresa")
public record CompanyScoreResponseDTO(

        @Schema(description = "ID da empresa", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID companyId,

        @Schema(description = "Pontuação atual", example = "820.0")
        double score
) {
}
