package com.volta.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Empresa posicionada no ranking")
public class EmpresaRankingDTO {

    @Schema(description = "Posição no ranking (1º, 2º...)", example = "1")
    private Integer posicao;

    @Schema(description = "UUID da empresa", example = "550e8400-e29b-41d4-a716-446655440000")
    private String companyUuid;

    @Schema(description = "Pontuação atual", example = "950.0")
    private Double score;
}
