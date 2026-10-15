package com.volta.ranking.repository;

import com.volta.ranking.dto.ranking.ScoreHistoryResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreHistoryRepositoryTest {

    private static final String PREFIX = "ranking:history";
    private static final String COMPANY_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String KEY = PREFIX + ":" + COMPANY_ID;

    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final ListOperations<String, String> listOperations = mock(ListOperations.class);
    private final ScoreHistoryRepository repository =
            new ScoreHistoryRepository(redisTemplate, JsonMapper.builder().build());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repository, "historyKeyPrefix", PREFIX);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void savesEntryOnTopAndKeepsOnlyTheLatestOnes() {
        Instant changedAt = Instant.parse("2026-10-14T15:30:00Z");
        ScoreHistoryResponseDTO entry = new ScoreHistoryResponseDTO(changedAt, 950.0, "Collection completed", "manager");

        repository.save(COMPANY_ID, entry);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(listOperations).leftPush(eq(KEY), json.capture());
        assertThat(json.getValue())
                .contains("\"score\":950.0")
                .contains("\"reason\":\"Collection completed\"")
                .contains("\"author\":\"manager\"");
        verify(listOperations).trim(KEY, 0, ScoreHistoryRepository.MAX_ENTRIES_PER_COMPANY - 1L);
    }

    @Test
    void findsEntriesInStoredOrder() {
        Instant changedAt = Instant.parse("2026-10-14T15:30:00Z");
        when(listOperations.range(KEY, 0, 1)).thenReturn(List.of(
                "{\"changedAt\":\"" + changedAt + "\",\"score\":900.0,\"reason\":\"B\",\"author\":\"x\"}",
                "{\"changedAt\":\"" + changedAt + "\",\"reason\":\"Removed from ranking\",\"author\":\"admin\"}"));

        List<ScoreHistoryResponseDTO> history = repository.findByCompany(COMPANY_ID, 2);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).score()).isEqualTo(900.0);
        assertThat(history.get(0).changedAt()).isEqualTo(changedAt);
        assertThat(history.get(1).score()).isNull();
        assertThat(history.get(1).reason()).isEqualTo("Removed from ranking");
    }

    @Test
    void returnsEmptyListWhenRedisReturnsNull() {
        when(listOperations.range(KEY, 0, 19)).thenReturn(null);

        assertThat(repository.findByCompany(COMPANY_ID, 20)).isEmpty();
    }
}
