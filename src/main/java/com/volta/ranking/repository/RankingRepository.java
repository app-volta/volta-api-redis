package com.volta.ranking.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Repositório de acesso direto ao Redis ZSET.
 *
 * Conforme documento de modelagem VOLTA:
 *   Key    → ranking:companies
 *   Member → UUID da empresa
 *   Score  → Pontuação (Double)
 *
 * Operações implementadas:
 *   ZADD         → adicionarOuAtualizar
 *   ZREVRANGE    → consultarRankingCompleto / consultarTopN
 *   ZSCORE       → consultarPontuacao
 *   ZREVRANK     → consultarPosicao
 *   ZREM         → remover
 *   ZCARD        → contarTotal
 */
@Repository
@RequiredArgsConstructor
public class RankingRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${volta.redis.keys.ranking}")
    private String rankingKey;

    /** ZADD ranking:companies {score} "{uuid}" */
    public Boolean adicionarOuAtualizar(String companyUuid, Double score) {
        return redisTemplate.opsForZSet().add(rankingKey, companyUuid, score);
    }

    /** ZREVRANGE ranking:companies 0 -1 WITHSCORES */
    public Set<ZSetOperations.TypedTuple<String>> consultarRankingCompleto() {
        return redisTemplate.opsForZSet().reverseRangeWithScores(rankingKey, 0, -1);
    }

    /** ZREVRANGE ranking:companies 0 {limit-1} WITHSCORES */
    public Set<ZSetOperations.TypedTuple<String>> consultarTopN(int limit) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(rankingKey, 0, limit - 1);
    }

    /** ZSCORE ranking:companies "{uuid}" */
    public Double consultarPontuacao(String companyUuid) {
        return redisTemplate.opsForZSet().score(rankingKey, companyUuid);
    }

    /**
     * ZREVRANK ranking:companies "{uuid}"
     * Redis retorna 0-based; a service converte para 1-based antes de retornar.
     */
    public Long consultarPosicao(String companyUuid) {
        return redisTemplate.opsForZSet().reverseRank(rankingKey, companyUuid);
    }

    /** ZREM ranking:companies "{uuid}" */
    public Long remover(String companyUuid) {
        return redisTemplate.opsForZSet().remove(rankingKey, companyUuid);
    }

    /** ZCARD ranking:companies */
    public Long contarTotal() {
        return redisTemplate.opsForZSet().zCard(rankingKey);
    }

    public boolean existe(String companyUuid) {
        return consultarPontuacao(companyUuid) != null;
    }
}
