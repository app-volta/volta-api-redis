package com.volta.ranking.dto.ranking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Dados para adicionar ou atualizar a pontuação de uma empresa")
public record ScoreUpdateRequestDTO(

        @Schema(
                description = "ID da empresa, o mesmo UUID do PostgreSQL da volta-api",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @NotNull(message = "Company id is required")
        UUID companyId,

        @Schema(description = "Pontuação calculada com base nos indicadores do Volta (0 a 1000)", example = "950.0")
        @NotNull(message = "Score is required")
        @DecimalMin(value = "0.0", message = "Score must be at least 0")
        @DecimalMax(value = "1000.0", message = "Score must be at most 1000")
        Double score,

        @Schema(description = "Motivo do recálculo, gravado no histórico", example = "Coleta concluída no setor de produção")
        @Size(max = 255, message = "Reason must have at most 255 characters")
        String reason
) {
}
