package com.volta.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Dados para adicionar ou atualizar a pontuação de uma empresa")
public class ScoreUpdateRequestDTO {

    @Schema(
        description = "UUID da empresa — mesmo UUID do PostgreSQL da volta-api",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String companyUuid;

    @Schema(description = "Pontuação calculada com base nos indicadores VOLTA (0–1000)", example = "950.0")
    private Double score;

    @Schema(description = "Motivo do recálculo (auditoria)", example = "Coleta concluída no setor de produção")
    private String motivo;
}
