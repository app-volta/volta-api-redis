package com.volta.ranking.mapper;

import com.volta.ranking.dto.ranking.CompanyRankingResponseDTO;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class RankingMapper {

    public List<CompanyRankingResponseDTO> toResponses(Set<ZSetOperations.TypedTuple<String>> tuples, long offset) {
        List<CompanyRankingResponseDTO> responses = new ArrayList<>();

        if (tuples == null) {
            return responses;
        }

        long position = offset + 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            UUID companyId = UUID.fromString(tuple.getValue());
            double score = tuple.getScore();

            responses.add(new CompanyRankingResponseDTO(Math.toIntExact(position), companyId, score));
            position++;
        }

        return responses;
    }
}
