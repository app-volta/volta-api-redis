package com.volta.ranking.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RankingRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${volta.redis.keys.ranking}")
    private String rankingKey;

    public Boolean save(String companyId, double score) {
        return redisTemplate.opsForZSet().add(rankingKey, companyId, score);
    }

    public Set<ZSetOperations.TypedTuple<String>> findRange(long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(rankingKey, start, end);
    }

    public Double findScore(String companyId) {
        return redisTemplate.opsForZSet().score(rankingKey, companyId);
    }

    public Long findPosition(String companyId) {
        return redisTemplate.opsForZSet().reverseRank(rankingKey, companyId);
    }

    public Long remove(String companyId) {
        return redisTemplate.opsForZSet().remove(rankingKey, companyId);
    }

    public Long count() {
        return redisTemplate.opsForZSet().zCard(rankingKey);
    }
}
