package com.volta.ranking.usecase;

import com.volta.ranking.dto.ranking.CompanyPositionResponseDTO;
import com.volta.ranking.dto.ranking.CompanyRankingResponseDTO;
import com.volta.ranking.dto.ranking.CompanyScoreResponseDTO;
import com.volta.ranking.dto.ranking.RankingTotalResponseDTO;
import com.volta.ranking.dto.ranking.ScoreBatchRequestDTO;
import com.volta.ranking.dto.ranking.ScoreBatchResponseDTO;
import com.volta.ranking.dto.ranking.ScoreHistoryResponseDTO;
import com.volta.ranking.dto.ranking.ScoreUpdateRequestDTO;
import com.volta.ranking.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface RankingUseCase {

    void updateScore(ScoreUpdateRequestDTO dto, AuthenticatedUser author);

    ScoreBatchResponseDTO loadScores(ScoreBatchRequestDTO dto, AuthenticatedUser author);

    List<CompanyRankingResponseDTO> getRanking(int page, int size);

    List<CompanyRankingResponseDTO> getTop(int limit);

    CompanyPositionResponseDTO getPosition(UUID companyId, AuthenticatedUser author);

    CompanyScoreResponseDTO getScore(UUID companyId, AuthenticatedUser author);

    List<ScoreHistoryResponseDTO> getHistory(UUID companyId, int limit, AuthenticatedUser author);

    RankingTotalResponseDTO getTotal();

    void remove(UUID companyId, AuthenticatedUser author);
}
