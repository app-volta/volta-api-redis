package com.volta.ranking.dto.ranking;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Total de empresas no ranking")
public record RankingTotalResponseDTO(

        @Schema(description = "Quantidade de empresas", example = "42")
        long total
) {
}
