package com.volta.ranking.service;

import com.volta.ranking.dto.NearbyCooperativeDTO;
import com.volta.ranking.repository.CooperativeGeoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.geo.GeoResults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CooperativeGeoService {

    private final CooperativeGeoRepository cooperativeGeoRepository;

    public void saveLocation(UUID cooperativeUuid, BigDecimal latitude, BigDecimal longitude) {
        cooperativeGeoRepository.saveLocation(cooperativeUuid, latitude, longitude);
    }

    public void removeLocation(UUID cooperativeUuid) {
        cooperativeGeoRepository.removeLocation(cooperativeUuid);
    }

    public List<NearbyCooperativeDTO> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            int limit
    ) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = cooperativeGeoRepository.searchNearby(
                latitude, longitude, radiusKm, limit
        );
        if (results == null) return List.of();

        return results.getContent().stream()
                .map(result -> new NearbyCooperativeDTO(
                        UUID.fromString(result.getContent().getName()),
                        result.getDistance().getValue()
                ))
                .toList();
    }
}
