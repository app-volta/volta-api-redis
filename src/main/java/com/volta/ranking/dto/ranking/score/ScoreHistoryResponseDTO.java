package com.volta.ranking.dto.ranking.score;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Alteração registrada no score de uma empresa")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScoreHistoryResponseDTO(

        @Schema(description = "Momento da alteração, em UTC", example = "2026-10-14T15:30:00Z")
        Instant changedAt,

        @Schema(description = "Novo score. Ausente quando a empresa foi removida do ranking", example = "950.0")
        Double score,

        @Schema(description = "Motivo informado na alteração", example = "Coleta concluída no setor de produção")
        String reason,

        @Schema(description = "Usuário que fez a alteração", example = "manager@volta.com")
        String author
) {
}
