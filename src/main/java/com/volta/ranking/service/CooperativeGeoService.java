package com.volta.ranking.service;

import com.volta.ranking.dto.cooperative.CooperativeLocationRequestDTO;
import com.volta.ranking.dto.cooperative.NearbyCooperativeResponseDTO;
import com.volta.ranking.exception.InvalidRequestException;
import com.volta.ranking.mapper.CooperativeGeoMapper;
import com.volta.ranking.repository.CooperativeGeoRepository;
import com.volta.ranking.usecase.CooperativeGeoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CooperativeGeoService implements CooperativeGeoUseCase {

    private final CooperativeGeoRepository cooperativeGeoRepository;
    private final CooperativeGeoMapper cooperativeGeoMapper;

    public void saveLocation(UUID cooperativeId, CooperativeLocationRequestDTO dto) {
        cooperativeGeoRepository.saveLocation(cooperativeId, dto.latitude(), dto.longitude());
    }

    public void removeLocation(UUID cooperativeId) {
        cooperativeGeoRepository.removeLocation(cooperativeId);
    }

    public List<NearbyCooperativeResponseDTO> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            int limit
    ) {
        if (latitude.abs().compareTo(new BigDecimal("85.051128")) > 0) {
            throw new InvalidRequestException("Latitude must be between -85.051128 and 85.051128");
        }
        if (longitude.abs().compareTo(new BigDecimal("180")) > 0) {
            throw new InvalidRequestException("Longitude must be between -180 and 180");
        }
        if (radiusKm.signum() <= 0 || radiusKm.compareTo(new BigDecimal("20000")) > 0) {
            throw new InvalidRequestException("Radius must be greater than 0 and at most 20000");
        }
        if (limit < 1 || limit > 100) {
            throw new InvalidRequestException("Limit must be between 1 and 100");
        }

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                cooperativeGeoRepository.searchNearby(latitude, longitude, radiusKm, limit);

        return cooperativeGeoMapper.toResponses(results);
    }
}
