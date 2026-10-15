package com.volta.ranking.service;

import com.volta.ranking.dto.ranking.CompanyPositionResponseDTO;
import com.volta.ranking.dto.ranking.CompanyRankingResponseDTO;
import com.volta.ranking.dto.ranking.CompanyScoreResponseDTO;
import com.volta.ranking.dto.ranking.RankingTotalResponseDTO;
import com.volta.ranking.dto.ranking.ScoreBatchRequestDTO;
import com.volta.ranking.dto.ranking.ScoreBatchResponseDTO;
import com.volta.ranking.dto.ranking.ScoreHistoryResponseDTO;
import com.volta.ranking.dto.ranking.ScoreUpdateRequestDTO;
import com.volta.ranking.exception.InvalidRequestException;
import com.volta.ranking.exception.ResourceNotFoundException;
import com.volta.ranking.mapper.RankingMapper;
import com.volta.ranking.repository.RankingRepository;
import com.volta.ranking.repository.ScoreHistoryRepository;
import com.volta.ranking.security.AuthenticatedUser;
import com.volta.ranking.usecase.RankingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService implements RankingUseCase {

    static final String BATCH_LOAD_REASON = "Batch load";
    static final String REMOVAL_REASON = "Removed from ranking";

    private static final int MAX_PAGE = 100_000;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String RANKING_ENTRY = "Ranking entry";

    private final RankingRepository rankingRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final RankingMapper rankingMapper;

    public void updateScore(ScoreUpdateRequestDTO dto, AuthenticatedUser author) {
        validateCompanyAccess(dto.companyId(), author);

        saveScore(dto.companyId(), dto.score(), dto.reason(), author);

        log.info("Score {} saved for company {} by {}", dto.score(), dto.companyId(), author.getUsername());
    }

    public ScoreBatchResponseDTO loadScores(ScoreBatchRequestDTO dto, AuthenticatedUser author) {
        for (ScoreUpdateRequestDTO item : dto.scores()) {
            String reason = item.reason();

            if (!StringUtils.hasText(reason)) {
                reason = BATCH_LOAD_REASON;
            }

            saveScore(item.companyId(), item.score(), reason, author);
        }

        int saved = dto.scores().size();
        log.info("Batch load of {} scores by {}", saved, author.getUsername());
        return new ScoreBatchResponseDTO(saved);
    }

    public List<CompanyRankingResponseDTO> getRanking(int page, int size) {
        if (page < 0 || page > MAX_PAGE) {
            throw new InvalidRequestException("Page must be between 0 and " + MAX_PAGE);
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
            throw new ResourceNotFoundException(RANKING_ENTRY);
        }

        long total = rankingRepository.count();
        long position = zeroBasedPosition + 1;

        return new CompanyPositionResponseDTO(companyId, position, score, total);
    }

    public CompanyScoreResponseDTO getScore(UUID companyId, AuthenticatedUser author) {
        validateCompanyAccess(companyId, author);

        Double score = rankingRepository.findScore(companyId.toString());

        if (score == null) {
            throw new ResourceNotFoundException(RANKING_ENTRY);
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
            throw new ResourceNotFoundException(RANKING_ENTRY);
        }

        ScoreHistoryResponseDTO entry = new ScoreHistoryResponseDTO(
                Instant.now(), null, REMOVAL_REASON, author.getUsername());
        scoreHistoryRepository.save(companyId.toString(), entry);

        log.info("Company {} removed from ranking by {}", companyId, author.getUsername());
    }

    private void saveScore(UUID companyId, double score, String reason, AuthenticatedUser author) {
        String member = companyId.toString();
        rankingRepository.save(member, score);

        ScoreHistoryResponseDTO entry = new ScoreHistoryResponseDTO(
                Instant.now(), score, reason, author.getUsername());
        scoreHistoryRepository.save(member, entry);
    }

    private void validateLimit(int value, String name) {
        if (value < MIN_LIMIT || value > MAX_LIMIT) {
            throw new InvalidRequestException(name + " must be between " + MIN_LIMIT + " and " + MAX_LIMIT);
        }
    }

    private void validateCompanyAccess(UUID companyId, AuthenticatedUser author) {
        if (ADMIN_ROLE.equals(author.role())) {
            return;
        }

        if (!companyId.equals(author.companyId())) {
            throw new ResourceNotFoundException(RANKING_ENTRY);
        }
    }
}
