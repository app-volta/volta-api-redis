package com.volta.ranking.dto.cooperative;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Coordenadas da cooperativa")
public record CooperativeLocationRequestDTO(

        @Schema(description = "Latitude, entre -85.051128 e 85.051128, com até seis casas decimais", example = "-23.550520")
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-85.051128", message = "Latitude must be at least -85.051128")
        @DecimalMax(value = "85.051128", message = "Latitude must be at most 85.051128")
        @Digits(integer = 3, fraction = 6, message = "Latitude must have at most 6 decimal places")
        BigDecimal latitude,

        @Schema(description = "Longitude, entre -180 e 180, com até seis casas decimais", example = "-46.633308")
        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "Longitude must be at most 180")
        @Digits(integer = 3, fraction = 6, message = "Longitude must have at most 6 decimal places")
        BigDecimal longitude
) {
}
