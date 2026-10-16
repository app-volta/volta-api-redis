package com.volta.ranking.dto.scoring;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Cooperativas aptas a receber a ocorrência, ordenadas pelo maior score")
public record IncidentCooperativeScoresResponseDTO(

        @Schema(description = "ID da ocorrência", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID incidentId,

        @Schema(description = "Prioridade da ocorrência", example = "HIGH")
        String priority,

        @Schema(description = "Categoria do resíduo", example = "PERIGOSO")
        String wasteCategory,

        @Schema(
                description = "Verdadeiro quando o tipo de resíduo, a contaminação informada ou a análise da IA "
                        + "indicam risco HIGH",
                example = "true"
        )
        boolean highRisk,

        @Schema(description = "Raio de busca usado, em quilômetros", example = "100")
        double radiusKm,

        @Schema(description = "Cooperativas ordenadas pelo maior score")
        List<CooperativeScoreResponseDTO> cooperatives
) {
}
