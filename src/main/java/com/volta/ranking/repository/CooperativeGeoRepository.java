package com.volta.ranking.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CooperativeGeoRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${volta.redis.keys.cooperatives}")
    private String cooperativesKey;

    public void saveLocation(UUID cooperativeId, BigDecimal latitude, BigDecimal longitude) {
        Point point = new Point(longitude.doubleValue(), latitude.doubleValue());
        redisTemplate.opsForGeo().add(cooperativesKey, point, cooperativeId.toString());
    }

    public void removeLocation(UUID cooperativeId) {
        redisTemplate.opsForGeo().remove(cooperativesKey, cooperativeId.toString());
    }

    public GeoResults<RedisGeoCommands.GeoLocation<String>> searchNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            int limit
    ) {
        GeoOperations<String, String> geoOperations = redisTemplate.opsForGeo();
        GeoReference<String> origin = GeoReference.fromCoordinate(longitude.doubleValue(), latitude.doubleValue());
        Distance radius = new Distance(radiusKm.doubleValue(), Metrics.KILOMETERS);
        RedisGeoCommands.GeoSearchCommandArgs arguments = RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                .includeDistance()
                .sortAscending()
                .limit(limit);

        return geoOperations.search(cooperativesKey, origin, radius, arguments);
    }
}
