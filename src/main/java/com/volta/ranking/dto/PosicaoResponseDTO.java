package com.volta.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Posição e pontuação de uma empresa específica no ranking")
public class PosicaoResponseDTO {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private String companyUuid;

    @Schema(description = "Posição no ranking (começa em 1)", example = "3")
    private Long posicao;

    @Schema(example = "820.0")
    private Double score;

    @Schema(description = "Total de empresas no ranking", example = "42")
    private Long totalEmpresas;
}
