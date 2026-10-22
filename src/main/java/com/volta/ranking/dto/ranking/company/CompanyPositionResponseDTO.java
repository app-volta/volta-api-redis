package com.volta.ranking.dto.ranking.company;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Posição e pontuação de uma empresa no ranking")
public record CompanyPositionResponseDTO(

        @Schema(description = "ID da empresa", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID companyId,

        @Schema(description = "Posição no ranking, começando em 1", example = "3")
        long position,

        @Schema(description = "Pontuação atual", example = "820.0")
        double score,

        @Schema(description = "Total de empresas no ranking", example = "42")
        long totalCompanies
) {
}
