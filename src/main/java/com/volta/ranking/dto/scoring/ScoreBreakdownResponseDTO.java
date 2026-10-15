package com.volta.ranking.dto.scoring;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contribuição de cada critério para o score, de 0 a 100")
public record ScoreBreakdownResponseDTO(

        @Schema(description = "Proximidade: 100 na origem e 0 na borda do raio", example = "85.5")
        double distance,

        @Schema(description = "Nota média da cooperativa; cooperativa sem avaliação recebe um valor neutro", example = "90.0")
        double rating,

        @Schema(description = "Adequação ao risco: 100 quando atende o risco da ocorrência", example = "100.0")
        double fit
) {
}
