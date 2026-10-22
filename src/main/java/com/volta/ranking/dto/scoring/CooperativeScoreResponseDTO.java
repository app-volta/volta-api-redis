package com.volta.ranking.dto.scoring;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Score de uma cooperativa para a ocorrência")
public record CooperativeScoreResponseDTO(

        @Schema(description = "ID da cooperativa", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID cooperativeId,

        @Schema(description = "Nome da cooperativa", example = "Cooperativa Recicla Sul")
        String name,

        @Schema(description = "Score final, de 0 a 100", example = "87.35")
        double score,

        @Schema(description = "Distância em linha reta da origem da ocorrência, em quilômetros", example = "12.4")
        double distanceKm,

        @Schema(description = "Contribuição de cada critério")
        ScoreBreakdownResponseDTO breakdown
) {
}
