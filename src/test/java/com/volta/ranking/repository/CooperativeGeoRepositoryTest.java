package com.volta.ranking.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CooperativeGeoRepositoryTest {

    private static final String KEY = "geo:cooperatives";
    private static final UUID COOPERATIVE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final GeoOperations<String, String> geoOperations = mock(GeoOperations.class);
    private final CooperativeGeoRepository repository = new CooperativeGeoRepository(redisTemplate);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repository, "cooperativesKey", KEY);
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
    }

    @Test
    void savesLocationWithLongitudeAsXAndLatitudeAsY() {
        repository.saveLocation(COOPERATIVE_ID, new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));

        ArgumentCaptor<Point> point = ArgumentCaptor.forClass(Point.class);
        verify(geoOperations).add(eq(KEY), point.capture(), eq(COOPERATIVE_ID.toString()));
        assertThat(point.getValue().getX()).isEqualTo(-46.633308);
        assertThat(point.getValue().getY()).isEqualTo(-23.550520);
    }

    @Test
    void searchesByLongitudeLatitudeAndRadiusInKilometers() {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = mock(GeoResults.class);
        when(geoOperations.search(
                eq(KEY), any(GeoReference.class), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class)
        )).thenReturn(results);

        GeoResults<RedisGeoCommands.GeoLocation<String>> found = repository.searchNearby(
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"), new BigDecimal("50"), 20);

        assertThat(found).isSameAs(results);

        ArgumentCaptor<GeoReference> reference = ArgumentCaptor.forClass(GeoReference.class);
        ArgumentCaptor<Distance> distance = ArgumentCaptor.forClass(Distance.class);
        verify(geoOperations).search(
                eq(KEY), reference.capture(), distance.capture(), any(RedisGeoCommands.GeoSearchCommandArgs.class));

        GeoReference.GeoCoordinateReference<String> coordinate =
                (GeoReference.GeoCoordinateReference<String>) reference.getValue();
        assertThat(coordinate.getLongitude()).isEqualTo(-46.633308);
        assertThat(coordinate.getLatitude()).isEqualTo(-23.550520);
        assertThat(distance.getValue().getValue()).isEqualTo(50.0);
    }

    @Test
    void removesCooperativeFromTheGeoIndex() {
        repository.removeLocation(COOPERATIVE_ID);

        verify(geoOperations).remove(KEY, COOPERATIVE_ID.toString());
    }
}
