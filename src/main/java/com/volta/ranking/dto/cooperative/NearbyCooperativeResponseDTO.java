package com.volta.ranking.dto.cooperative;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Cooperativa encontrada na busca por proximidade")
public record NearbyCooperativeResponseDTO(

        @Schema(description = "ID da cooperativa", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID cooperativeId,

        @Schema(description = "Distância em linha reta, em quilômetros", example = "4.25")
        double distanceKm
) {
}
