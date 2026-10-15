package com.volta.ranking.dto.voltaapi;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CooperativeDataDTO(
        UUID id,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal averageRating,
        List<String> specialties
) {
}
