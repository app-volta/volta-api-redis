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

    private static final BigDecimal MAX_LATITUDE = new BigDecimal("85.051128");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final BigDecimal MAX_RADIUS_KM = new BigDecimal("20000");
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;

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
        validateRange(latitude, MAX_LATITUDE, "Latitude");
        validateRange(longitude, MAX_LONGITUDE, "Longitude");

        if (radiusKm.signum() <= 0 || radiusKm.compareTo(MAX_RADIUS_KM) > 0) {
            throw new InvalidRequestException("Radius must be greater than 0 and at most " + MAX_RADIUS_KM);
        }
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new InvalidRequestException("Limit must be between " + MIN_LIMIT + " and " + MAX_LIMIT);
        }

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                cooperativeGeoRepository.searchNearby(latitude, longitude, radiusKm, limit);

        return cooperativeGeoMapper.toResponses(results);
    }

    private void validateRange(BigDecimal value, BigDecimal maxAbsolute, String name) {
        if (value.abs().compareTo(maxAbsolute) > 0) {
            throw new InvalidRequestException(name + " must be between -" + maxAbsolute + " and " + maxAbsolute);
        }
    }
}
