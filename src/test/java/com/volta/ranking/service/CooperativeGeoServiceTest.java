package com.volta.ranking.service;

import com.volta.ranking.dto.cooperative.CooperativeLocationRequestDTO;
import com.volta.ranking.dto.cooperative.NearbyCooperativeResponseDTO;
import com.volta.ranking.exception.InvalidRequestException;
import com.volta.ranking.mapper.CooperativeGeoMapper;
import com.volta.ranking.repository.CooperativeGeoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CooperativeGeoServiceTest {

    private static final UUID COOPERATIVE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private final CooperativeGeoRepository repository = mock(CooperativeGeoRepository.class);
    private final CooperativeGeoService service = new CooperativeGeoService(repository, new CooperativeGeoMapper());

    @Test
    void savesTheLocationOfACooperative() {
        CooperativeLocationRequestDTO dto = new CooperativeLocationRequestDTO(
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));

        service.saveLocation(COOPERATIVE_ID, dto);

        verify(repository).saveLocation(COOPERATIVE_ID, dto.latitude(), dto.longitude());
    }

    @Test
    void removesTheLocationOfACooperative() {
        service.removeLocation(COOPERATIVE_ID);

        verify(repository).removeLocation(COOPERATIVE_ID);
    }

    @Test
    void findNearbyReturnsTheIdAndTheDistanceInKilometers() {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = mock(GeoResults.class);
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult = mock(GeoResult.class);
        RedisGeoCommands.GeoLocation<String> location = mock(RedisGeoCommands.GeoLocation.class);
        when(results.getContent()).thenReturn(List.of(geoResult));
        when(geoResult.getContent()).thenReturn(location);
        when(location.getName()).thenReturn(COOPERATIVE_ID.toString());
        when(geoResult.getDistance()).thenReturn(new Distance(4.25, Metrics.KILOMETERS));
        when(repository.searchNearby(any(), any(), any(), anyInt())).thenReturn(results);

        List<NearbyCooperativeResponseDTO> nearby = service.findNearby(
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"), new BigDecimal("50"), 20);

        assertThat(nearby).containsExactly(new NearbyCooperativeResponseDTO(COOPERATIVE_ID, 4.25));
    }

    @Test
    void findNearbyReturnsEmptyListWhenRedisReturnsNoResults() {
        when(repository.searchNearby(any(), any(), any(), anyInt())).thenReturn(null);

        List<NearbyCooperativeResponseDTO> nearby = service.findNearby(
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"), new BigDecimal("50"), 20);

        assertThat(nearby).isEmpty();
    }

    @Test
    void findNearbyRejectsCoordinatesOutOfRange() {
        BigDecimal valid = new BigDecimal("10");
        BigDecimal radius = new BigDecimal("50");

        assertThatThrownBy(() -> service.findNearby(new BigDecimal("85.051129"), valid, radius, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Latitude");
        assertThatThrownBy(() -> service.findNearby(valid, new BigDecimal("-180.1"), radius, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Longitude");

        verifyNoInteractions(repository);
    }

    @Test
    void findNearbyRejectsRadiusOutOfRange() {
        BigDecimal valid = new BigDecimal("10");

        assertThatThrownBy(() -> service.findNearby(valid, valid, BigDecimal.ZERO, 20))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Radius");
        assertThatThrownBy(() -> service.findNearby(valid, valid, new BigDecimal("20000.1"), 20))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void findNearbyRejectsLimitOutOfRange() {
        BigDecimal valid = new BigDecimal("10");

        assertThatThrownBy(() -> service.findNearby(valid, valid, valid, 0))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Limit");
        assertThatThrownBy(() -> service.findNearby(valid, valid, valid, 101))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(repository);
    }
}
