package com.volta.ranking.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RankingRepositoryTest {

    private static final String KEY = "ranking:companies";
    private static final String COMPANY_ID = "550e8400-e29b-41d4-a716-446655440000";

    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
    private final RankingRepository repository = new RankingRepository(redisTemplate);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repository, "rankingKey", KEY);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void savesScoreWithZadd() {
        repository.save(COMPANY_ID, 950.0);

        verify(zSetOperations).add(KEY, COMPANY_ID, 950.0);
    }

    @Test
    void findsRangeFromHighestToLowestScore() {
        repository.findRange(10, 19);

        verify(zSetOperations).reverseRangeWithScores(KEY, 10, 19);
    }

    @Test
    void findsScoreAndPosition() {
        when(zSetOperations.score(KEY, COMPANY_ID)).thenReturn(820.0);
        when(zSetOperations.reverseRank(KEY, COMPANY_ID)).thenReturn(2L);

        assertThat(repository.findScore(COMPANY_ID)).isEqualTo(820.0);
        assertThat(repository.findPosition(COMPANY_ID)).isEqualTo(2L);
    }

    @Test
    void removesAndCounts() {
        when(zSetOperations.remove(KEY, COMPANY_ID)).thenReturn(1L);
        when(zSetOperations.zCard(KEY)).thenReturn(7L);

        assertThat(repository.remove(COMPANY_ID)).isEqualTo(1L);
        assertThat(repository.count()).isEqualTo(7L);
    }
}
