package com.volta.ranking.repository;

import com.volta.ranking.dto.ranking.ScoreHistoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ScoreHistoryRepository {

    static final int MAX_ENTRIES_PER_COMPANY = 100;

    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;

    @Value("${volta.redis.keys.ranking-history}")
    private String historyKeyPrefix;

    public void save(String companyId, ScoreHistoryResponseDTO entry) {
        String key = buildKey(companyId);
        String json = jsonMapper.writeValueAsString(entry);

        redisTemplate.opsForList().leftPush(key, json);
        redisTemplate.opsForList().trim(key, 0, MAX_ENTRIES_PER_COMPANY - 1L);
    }

    public List<ScoreHistoryResponseDTO> findByCompany(String companyId, int limit) {
        List<String> items = redisTemplate.opsForList().range(buildKey(companyId), 0, limit - 1L);
        List<ScoreHistoryResponseDTO> history = new ArrayList<>();

        if (items == null) {
            return history;
        }

        for (String item : items) {
            history.add(jsonMapper.readValue(item, ScoreHistoryResponseDTO.class));
        }

        return history;
    }

    private String buildKey(String companyId) {
        return historyKeyPrefix + ":" + companyId;
    }
}
