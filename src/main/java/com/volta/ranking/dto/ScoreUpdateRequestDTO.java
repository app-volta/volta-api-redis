package com.volta.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Dados para adicionar ou atualizar a pontuação de uma empresa")
public class ScoreUpdateRequestDTO {

    @NotBlank(message = "O UUID da empresa é obrigatório")
    @Schema(
        description = "UUID da empresa — mesmo UUID do PostgreSQL da volta-api",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "UUID da empresa inválido")
    private String companyUuid;

    @NotNull(message = "A pontuação é obrigatória")
    @DecimalMin(value = "0.0", message = "Pontuação mínima é 0")
    @DecimalMax(value = "1000.0", message = "Pontuação máxima é 1000")
    @Schema(description = "Pontuação calculada com base nos indicadores VOLTA (0–1000)", example = "950.0")
    private Double score;

    @Schema(description = "Motivo do recálculo (auditoria)", example = "Coleta concluída no setor de produção")
    @Size(max = 255, message = "Motivo deve ter no máximo 255 caracteres")
    private String motivo;
}
