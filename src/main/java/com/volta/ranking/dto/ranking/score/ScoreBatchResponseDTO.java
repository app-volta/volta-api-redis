package com.volta.ranking.dto.ranking.score;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da carga em lote")
public record ScoreBatchResponseDTO(

        @Schema(description = "Quantidade de scores gravados", example = "42")
        int saved
) {
}
