package com.volta.ranking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CooperativeLocationRequestDTO(
        @NotNull
        @DecimalMin("-85.051128")
        @DecimalMax("85.051128")
        @Digits(integer = 3, fraction = 6)
        BigDecimal latitude,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        @Digits(integer = 3, fraction = 6)
        BigDecimal longitude
) {
}
