package com.volta.ranking.service;

import com.volta.ranking.dto.NearbyCooperativeDTO;
import com.volta.ranking.repository.CooperativeGeoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisGeoCommands;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CooperativeGeoServiceTest {

    private static final UUID COOPERATIVE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private final CooperativeGeoRepository repository = mock(CooperativeGeoRepository.class);
    private final CooperativeGeoService service = new CooperativeGeoService(repository);

    @Test
    void findNearby_retornaUuidEDistanciaEmQuilometros() {
        @SuppressWarnings("unchecked")
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = mock(GeoResults.class);
        @SuppressWarnings("unchecked")
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult = mock(GeoResult.class);
        @SuppressWarnings("unchecked")
        RedisGeoCommands.GeoLocation<String> location = mock(RedisGeoCommands.GeoLocation.class);
        when(results.getContent()).thenReturn(List.of(geoResult));
        when(geoResult.getContent()).thenReturn(location);
        when(location.getName()).thenReturn(COOPERATIVE_UUID.toString());
        when(geoResult.getDistance()).thenReturn(new Distance(4.25, Metrics.KILOMETERS));
        when(repository.searchNearby(any(), any(), any(), anyInt())).thenReturn(results);

        List<NearbyCooperativeDTO> nearby = service.findNearby(
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"), new BigDecimal("50"), 20
        );

        assertThat(nearby).containsExactly(new NearbyCooperativeDTO(COOPERATIVE_UUID, 4.25));
    }

    @Test
    void findNearby_quandoRedisNaoRetornaResultados_devolveListaVazia() {
        when(repository.searchNearby(any(), any(), any(), anyInt())).thenReturn(null);

        assertThat(service.findNearby(
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"), new BigDecimal("50"), 20
        )).isEmpty();
    }
}
