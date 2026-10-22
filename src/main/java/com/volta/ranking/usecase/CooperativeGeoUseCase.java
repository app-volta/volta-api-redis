package com.volta.ranking.usecase;

import com.volta.ranking.dto.cooperative.CooperativeLocationRequestDTO;
import com.volta.ranking.dto.cooperative.NearbyCooperativeResponseDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CooperativeGeoUseCase {

    void saveLocation(UUID cooperativeId, CooperativeLocationRequestDTO dto);

    void removeLocation(UUID cooperativeId);

    List<NearbyCooperativeResponseDTO> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            int limit
    );
}
