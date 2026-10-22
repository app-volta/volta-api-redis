package com.volta.ranking.dto.voltaapi;

import java.math.BigDecimal;

public record CoordinatesDTO(
        BigDecimal latitude,
        BigDecimal longitude
) {
}
