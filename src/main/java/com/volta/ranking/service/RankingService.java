package com.volta.ranking.service;

import com.volta.ranking.dto.ranking.company.CompanyPositionResponseDTO;
import com.volta.ranking.dto.ranking.company.CompanyRankingResponseDTO;
import com.volta.ranking.dto.ranking.company.CompanyScoreResponseDTO;
import com.volta.ranking.dto.ranking.RankingTotalResponseDTO;
import com.volta.ranking.dto.ranking.score.ScoreBatchRequestDTO;
import com.volta.ranking.dto.ranking.score.ScoreBatchResponseDTO;
import com.volta.ranking.dto.ranking.score.ScoreHistoryResponseDTO;
import com.volta.ranking.dto.ranking.score.ScoreUpdateRequestDTO;
import com.volta.ranking.exception.InvalidRequestException;
import com.volta.ranking.exception.ResourceNotFoundException;
import com.volta.ranking.mapper.RankingMapper;
import com.volta.ranking.repository.RankingRepository;
import com.volta.ranking.repository.ScoreHistoryRepository;
import com.volta.ranking.security.AuthenticatedUser;
import com.volta.ranking.usecase.RankingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService implements RankingUseCase {

    private final RankingRepository rankingRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final RankingMapper rankingMapper;

    public void updateScore(ScoreUpdateRequestDTO dto, AuthenticatedUser author) {
        validateCompanyAccess(dto.companyId(), author);

        saveScore(dto.companyId(), dto.score(), dto.reason(), author);
    }

    public ScoreBatchResponseDTO loadScores(ScoreBatchRequestDTO dto, AuthenticatedUser author) {
        for (ScoreUpdateRequestDTO item : dto.scores()) {
            String reason = item.reason();

            if (!StringUtils.hasText(reason)) {
                reason = "Batch load";
            }

            saveScore(item.companyId(), item.score(), reason, author);
        }

        return new ScoreBatchResponseDTO(dto.scores().size());
    }

    public List<CompanyRankingResponseDTO> getRanking(int page, int size) {
        if (page < 0 || page > 100_000) {
            throw new InvalidRequestException("Page must be between 0 and 100000");
        }
        validateLimit(size, "Size");

        long start = (long) page * size;
        long end = start + size - 1;
        Set<ZSetOperations.TypedTuple<String>> tuples = rankingRepository.findRange(start, end);

        return rankingMapper.toResponses(tuples, start);
    }

    public List<CompanyRankingResponseDTO> getTop(int limit) {
        validateLimit(limit, "Limit");

        Set<ZSetOperations.TypedTuple<String>> tuples = rankingRepository.findRange(0, limit - 1L);

        return rankingMapper.toResponses(tuples, 0);
    }

    public CompanyPositionResponseDTO getPosition(UUID companyId, AuthenticatedUser author) {
        validateCompanyAccess(companyId, author);

        String member = companyId.toString();
        Long zeroBasedPosition = rankingRepository.findPosition(member);
        Double score = rankingRepository.findScore(member);

        if (zeroBasedPosition == null || score == null) {
            throw new ResourceNotFoundException("Ranking entry");
        }

        long total = rankingRepository.count();
        long position = zeroBasedPosition + 1;

        return new CompanyPositionResponseDTO(companyId, position, score, total);
    }

    public CompanyScoreResponseDTO getScore(UUID companyId, AuthenticatedUser author) {
        validateCompanyAccess(companyId, author);

        Double score = rankingRepository.findScore(companyId.toString());

        if (score == null) {
            throw new ResourceNotFoundException("Ranking entry");
        }

        return new CompanyScoreResponseDTO(companyId, score);
    }

    public List<ScoreHistoryResponseDTO> getHistory(UUID companyId, int limit, AuthenticatedUser author) {
        validateLimit(limit, "Limit");
        validateCompanyAccess(companyId, author);

        return scoreHistoryRepository.findByCompany(companyId.toString(), limit);
    }

    public RankingTotalResponseDTO getTotal() {
        long total = rankingRepository.count();
        return new RankingTotalResponseDTO(total);
    }

    public void remove(UUID companyId, AuthenticatedUser author) {
        Long removed = rankingRepository.remove(companyId.toString());

        if (removed == null || removed == 0) {
            throw new ResourceNotFoundException("Ranking entry");
        }

        ScoreHistoryResponseDTO entry = new ScoreHistoryResponseDTO(
                Instant.now(), null, "Removed from ranking", author.getUsername());
        scoreHistoryRepository.save(companyId.toString(), entry);
    }

    private void saveScore(UUID companyId, double score, String reason, AuthenticatedUser author) {
        String member = companyId.toString();
        rankingRepository.save(member, score);

        ScoreHistoryResponseDTO entry = new ScoreHistoryResponseDTO(
                Instant.now(), score, reason, author.getUsername()
        );
        scoreHistoryRepository.save(member, entry);
    }

    private void validateLimit(int value, String name) {
        if (value < 1 || value > 100) {
            throw new InvalidRequestException(name + " must be between 1 and 100");
        }
    }

    private void validateCompanyAccess(UUID companyId, AuthenticatedUser author) {
        if ("ADMIN".equals(author.role())) {
            return;
        }

        if (!companyId.equals(author.companyId())) {
            throw new ResourceNotFoundException("Ranking entry");
        }
    }
}
