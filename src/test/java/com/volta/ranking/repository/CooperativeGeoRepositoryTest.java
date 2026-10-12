package com.volta.ranking.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CooperativeGeoRepositoryTest {

    private static final String KEY = "geo:cooperatives";
    private static final UUID COOPERATIVE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final GeoOperations<String, String> geoOperations = mock(GeoOperations.class);
    private final CooperativeGeoRepository repository = new CooperativeGeoRepository(redisTemplate);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repository, "cooperativesKey", KEY);
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
    }

    @Test
    void saveLocation_usaLongitudeComoXELatitudeComoY() {
        repository.saveLocation(COOPERATIVE_UUID, new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));

        verify(geoOperations).add(eq(KEY), argThat(point ->
                point.getX() == -46.633308 && point.getY() == -23.550520), eq(COOPERATIVE_UUID.toString()));
    }

    @Test
    void searchNearby_usaLongitudeLatitudeRaioEmKmEArgumentosOrdenados() {
        @SuppressWarnings("unchecked")
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = mock(GeoResults.class);
        when(geoOperations.search(
                eq(KEY), any(GeoReference.class), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class)
        )).thenReturn(results);

        GeoResults<RedisGeoCommands.GeoLocation<String>> result = repository.searchNearby(
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"), new BigDecimal("50"), 20
        );

        assertThat(result).isSameAs(results);
        @SuppressWarnings("unchecked")
        var reference = org.mockito.ArgumentCaptor.forClass(GeoReference.class);
        var distance = org.mockito.ArgumentCaptor.forClass(Distance.class);
        var args = org.mockito.ArgumentCaptor.forClass(RedisGeoCommands.GeoSearchCommandArgs.class);
        verify(geoOperations).search(eq(KEY), reference.capture(), distance.capture(), args.capture());
        var coordinate = (GeoReference.GeoCoordinateReference<String>) reference.getValue();
        assertThat(coordinate.getLongitude()).isEqualTo(-46.633308);
        assertThat(coordinate.getLatitude()).isEqualTo(-23.550520);
        assertThat(distance.getValue().getValue()).isEqualTo(50.0);
    }

    @Test
    void removeLocation_removeOUuidDoIndiceGEO() {
        repository.removeLocation(COOPERATIVE_UUID);

        verify(geoOperations).remove(KEY, COOPERATIVE_UUID.toString());
    }
}
