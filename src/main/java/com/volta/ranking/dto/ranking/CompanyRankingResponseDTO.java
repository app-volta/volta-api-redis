package com.volta.ranking.dto.ranking;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Empresa posicionada no ranking")
public record CompanyRankingResponseDTO(

        @Schema(description = "Posição no ranking, começando em 1", example = "1")
        int position,

        @Schema(description = "ID da empresa", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID companyId,

        @Schema(description = "Pontuação atual", example = "950.0")
        double score
) {
}
