package com.volta.ranking.repository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CooperativeGeoRedisIntegrationTest {

    private static final String KEY = "geo:test:" + UUID.randomUUID();
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static CooperativeGeoRepository repository;
    private static final UUID NEAR = UUID.randomUUID();
    private static final UUID FAR = UUID.randomUUID();

    @BeforeAll
    static void connectToDisposableRedis() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        connectionFactory = new LettuceConnectionFactory(host, port);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        repository = new CooperativeGeoRepository(redisTemplate);
        ReflectionTestUtils.setField(repository, "cooperativesKey", KEY);
    }

    @AfterAll
    static void removeTestDataAndCloseConnection() {
        if (redisTemplate != null) redisTemplate.delete(KEY);
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @Test
    void searchNearby_aplicaRaioOrdemAscendenteELimiteNoRedisReal() {
        repository.saveLocation(NEAR, new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));
        repository.saveLocation(FAR, new BigDecimal("-23.550520"), new BigDecimal("-46.733308"));

        GeoResults<RedisGeoCommands.GeoLocation<String>> withinTwentyKm = repository.searchNearby(
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"), new BigDecimal("20"), 2);
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> results = withinTwentyKm.getContent();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getContent().getName()).isEqualTo(NEAR.toString());
        assertThat(results.get(0).getDistance().getValue()).isCloseTo(0.0,
                org.assertj.core.data.Offset.offset(0.01));
        assertThat(results.get(1).getContent().getName()).isEqualTo(FAR.toString());
        assertThat(results.get(1).getDistance().getValue()).isBetween(10.0, 12.0);

        assertThat(repository.searchNearby(new BigDecimal("-23.550520"), new BigDecimal("-46.633308"),
                new BigDecimal("20"), 1).getContent()).hasSize(1);
        assertThat(repository.searchNearby(new BigDecimal("-23.550520"), new BigDecimal("-46.633308"),
                new BigDecimal("2"), 10).getContent()).hasSize(1);
    }
}
