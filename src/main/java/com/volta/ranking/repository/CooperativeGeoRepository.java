package com.volta.ranking.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CooperativeGeoRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${volta.redis.keys.cooperatives}")
    private String cooperativesKey;

    public void saveLocation(UUID cooperativeUuid, BigDecimal latitude, BigDecimal longitude) {
        redisTemplate.opsForGeo().add(
                cooperativesKey,
                new Point(longitude.doubleValue(), latitude.doubleValue()),
                cooperativeUuid.toString()
        );
    }

    public void removeLocation(UUID cooperativeUuid) {
        redisTemplate.opsForGeo().remove(cooperativesKey, cooperativeUuid.toString());
    }

    public GeoResults<RedisGeoCommands.GeoLocation<String>> searchNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            int limit
    ) {
        GeoOperations<String, String> geo = redisTemplate.opsForGeo();
        return geo.search(
                cooperativesKey,
                GeoReference.fromCoordinate(longitude.doubleValue(), latitude.doubleValue()),
                new Distance(radiusKm.doubleValue(), Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit)
        );
    }
}
