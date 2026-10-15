package com.volta.ranking.service;

import com.volta.ranking.dto.ranking.CompanyPositionResponseDTO;
import com.volta.ranking.dto.ranking.CompanyRankingResponseDTO;
import com.volta.ranking.dto.ranking.CompanyScoreResponseDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RankingServiceTest {

    private static final UUID MY_COMPANY = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID OTHER_COMPANY = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

    private final RankingRepository rankingRepository = mock(RankingRepository.class);
    private final ScoreHistoryRepository scoreHistoryRepository = mock(ScoreHistoryRepository.class);

    private final AuthenticatedUser manager = user("manager@volta.com", "MANAGER");
    private final AuthenticatedUser employee = user("employee@volta.com", "EMPLOYEE");
    private final AuthenticatedUser admin = user("admin@volta.com", "ADMIN");

    private RankingService service;

    @BeforeEach
    void setUp() {
        service = new RankingService(rankingRepository, scoreHistoryRepository, new RankingMapper());
    }

    @Test
    void updateScoreSavesTheScoreAndRecordsTheHistory() {
        ScoreUpdateRequestDTO dto = new ScoreUpdateRequestDTO(MY_COMPANY, 950.0, "Collection completed");

        service.updateScore(dto, manager);

        verify(rankingRepository).save(MY_COMPANY.toString(), 950.0);
        ArgumentCaptor<ScoreHistoryResponseDTO> entry = ArgumentCaptor.forClass(ScoreHistoryResponseDTO.class);
        verify(scoreHistoryRepository).save(eq(MY_COMPANY.toString()), entry.capture());
        assertThat(entry.getValue().score()).isEqualTo(950.0);
        assertThat(entry.getValue().reason()).isEqualTo("Collection completed");
        assertThat(entry.getValue().author()).isEqualTo("manager@volta.com");
        assertThat(entry.getValue().changedAt()).isNotNull();
    }

    @Test
    void updateScoreOfAnotherCompanyIsNotFoundAndSavesNothing() {
        ScoreUpdateRequestDTO dto = new ScoreUpdateRequestDTO(OTHER_COMPANY, 950.0, null);

        assertThatThrownBy(() -> service.updateScore(dto, manager))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(rankingRepository, scoreHistoryRepository);
    }

    @Test
    void adminUpdatesTheScoreOfAnyCompany() {
        ScoreUpdateRequestDTO dto = new ScoreUpdateRequestDTO(OTHER_COMPANY, 400.0, null);

        service.updateScore(dto, admin);

        verify(rankingRepository).save(OTHER_COMPANY.toString(), 400.0);
    }

    @Test
    void loadScoresSavesAllAndUsesTheDefaultReasonWhenMissing() {
        ScoreBatchRequestDTO batch = new ScoreBatchRequestDTO(List.of(
                new ScoreUpdateRequestDTO(MY_COMPANY, 100.0, "Monthly recalculation"),
                new ScoreUpdateRequestDTO(OTHER_COMPANY, 200.0, null),
                new ScoreUpdateRequestDTO(UUID.randomUUID(), 300.0, "  ")));

        ScoreBatchResponseDTO response = service.loadScores(batch, admin);

        assertThat(response.saved()).isEqualTo(3);
        verify(rankingRepository).save(MY_COMPANY.toString(), 100.0);
        verify(rankingRepository).save(OTHER_COMPANY.toString(), 200.0);
        verify(rankingRepository, times(3)).save(anyString(), anyDouble());

        ArgumentCaptor<ScoreHistoryResponseDTO> entries = ArgumentCaptor.forClass(ScoreHistoryResponseDTO.class);
        verify(scoreHistoryRepository, times(3)).save(anyString(), entries.capture());
        assertThat(entries.getAllValues()).extracting(ScoreHistoryResponseDTO::reason)
                .containsExactly("Monthly recalculation", RankingService.BATCH_LOAD_REASON,
                        RankingService.BATCH_LOAD_REASON);
    }

    @Test
    void getRankingReturnsTheFirstPageWithPositionsStartingAtOne() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(tuple(first, 900.0));
        tuples.add(tuple(second, 850.0));
        when(rankingRepository.findRange(0, 1)).thenReturn(tuples);

        List<CompanyRankingResponseDTO> ranking = service.getRanking(0, 2);

        assertThat(ranking).containsExactly(
                new CompanyRankingResponseDTO(1, first, 900.0),
                new CompanyRankingResponseDTO(2, second, 850.0));
    }

    @Test
    void getRankingNumbersLaterPagesAfterThePreviousOnes() {
        UUID company = UUID.randomUUID();
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(tuple(company, 700.0));
        when(rankingRepository.findRange(20, 29)).thenReturn(tuples);

        List<CompanyRankingResponseDTO> ranking = service.getRanking(2, 10);

        assertThat(ranking).singleElement().satisfies(entry -> assertThat(entry.position()).isEqualTo(21));
    }

    @Test
    void getRankingReturnsEmptyListWhenRedisReturnsNull() {
        when(rankingRepository.findRange(0, 99)).thenReturn(null);

        assertThat(service.getRanking(0, 100)).isEmpty();
    }

    @Test
    void getRankingRejectsInvalidPageOrSize() {
        assertThatThrownBy(() -> service.getRanking(-1, 10)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.getRanking(100_001, 10)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.getRanking(0, 0)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.getRanking(0, 101)).isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(rankingRepository);
    }

    @Test
    void getTopReturnsTheBestCompaniesStartingAtPositionOne() {
        UUID company = UUID.randomUUID();
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(tuple(company, 999.0));
        when(rankingRepository.findRange(0, 0)).thenReturn(tuples);

        assertThat(service.getTop(1)).containsExactly(new CompanyRankingResponseDTO(1, company, 999.0));
    }

    @Test
    void getTopRejectsInvalidLimit() {
        assertThatThrownBy(() -> service.getTop(0)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.getTop(-5)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.getTop(101)).isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(rankingRepository);
    }

    @Test
    void getPositionConvertsTheZeroBasedRedisRankToOneBased() {
        when(rankingRepository.findPosition(MY_COMPANY.toString())).thenReturn(0L);
        when(rankingRepository.findScore(MY_COMPANY.toString())).thenReturn(920.0);
        when(rankingRepository.count()).thenReturn(3L);

        CompanyPositionResponseDTO position = service.getPosition(MY_COMPANY, employee);

        assertThat(position).isEqualTo(new CompanyPositionResponseDTO(MY_COMPANY, 1L, 920.0, 3L));
    }

    @Test
    void getPositionIsNotFoundWhenTheCompanyIsNotInTheRanking() {
        when(rankingRepository.findPosition(MY_COMPANY.toString())).thenReturn(null);
        when(rankingRepository.findScore(MY_COMPANY.toString())).thenReturn(null);

        assertThatThrownBy(() -> service.getPosition(MY_COMPANY, employee))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ranking entry not found");

        verify(rankingRepository, never()).count();
    }

    @Test
    void getPositionIsNotFoundWhenTheCompanyLeavesTheRankingBetweenReads() {
        when(rankingRepository.findPosition(MY_COMPANY.toString())).thenReturn(0L);
        when(rankingRepository.findScore(MY_COMPANY.toString())).thenReturn(null);

        assertThatThrownBy(() -> service.getPosition(MY_COMPANY, employee))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPositionOfAnotherCompanyIsNotFoundWithoutTouchingRedis() {
        assertThatThrownBy(() -> service.getPosition(OTHER_COMPANY, employee))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(rankingRepository);
    }

    @Test
    void adminReadsThePositionOfAnyCompany() {
        when(rankingRepository.findPosition(OTHER_COMPANY.toString())).thenReturn(4L);
        when(rankingRepository.findScore(OTHER_COMPANY.toString())).thenReturn(100.0);
        when(rankingRepository.count()).thenReturn(10L);

        assertThat(service.getPosition(OTHER_COMPANY, admin).position()).isEqualTo(5L);
    }

    @Test
    void getScoreReturnsTheScore() {
        when(rankingRepository.findScore(MY_COMPANY.toString())).thenReturn(640.5);

        CompanyScoreResponseDTO score = service.getScore(MY_COMPANY, employee);

        assertThat(score).isEqualTo(new CompanyScoreResponseDTO(MY_COMPANY, 640.5));
    }

    @Test
    void getScoreIsNotFoundWhenTheCompanyIsNotInTheRanking() {
        when(rankingRepository.findScore(MY_COMPANY.toString())).thenReturn(null);

        assertThatThrownBy(() -> service.getScore(MY_COMPANY, employee))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getScoreOfAnotherCompanyIsNotFound() {
        assertThatThrownBy(() -> service.getScore(OTHER_COMPANY, manager))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(rankingRepository);
    }

    @Test
    void getHistoryDelegatesToTheRepository() {
        List<ScoreHistoryResponseDTO> history = List.of(new ScoreHistoryResponseDTO(null, 10.0, "reason", "author"));
        when(scoreHistoryRepository.findByCompany(MY_COMPANY.toString(), 5)).thenReturn(history);

        assertThat(service.getHistory(MY_COMPANY, 5, employee)).isEqualTo(history);
    }

    @Test
    void getHistoryRejectsInvalidLimit() {
        assertThatThrownBy(() -> service.getHistory(MY_COMPANY, 0, employee))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.getHistory(MY_COMPANY, 101, employee))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(scoreHistoryRepository);
    }

    @Test
    void getHistoryOfAnotherCompanyIsNotFound() {
        assertThatThrownBy(() -> service.getHistory(OTHER_COMPANY, 5, employee))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(scoreHistoryRepository);
    }

    @Test
    void getTotalReturnsTheNumberOfCompanies() {
        when(rankingRepository.count()).thenReturn(7L);

        assertThat(service.getTotal().total()).isEqualTo(7L);
    }

    @Test
    void removeDeletesTheCompanyAndRecordsTheRemovalInTheHistory() {
        when(rankingRepository.remove(MY_COMPANY.toString())).thenReturn(1L);

        service.remove(MY_COMPANY, admin);

        ArgumentCaptor<ScoreHistoryResponseDTO> entry = ArgumentCaptor.forClass(ScoreHistoryResponseDTO.class);
        verify(scoreHistoryRepository).save(eq(MY_COMPANY.toString()), entry.capture());
        assertThat(entry.getValue().score()).isNull();
        assertThat(entry.getValue().reason()).isEqualTo(RankingService.REMOVAL_REASON);
        assertThat(entry.getValue().author()).isEqualTo("admin@volta.com");
    }

    @Test
    void removeIsNotFoundWhenTheCompanyIsNotInTheRanking() {
        when(rankingRepository.remove(MY_COMPANY.toString())).thenReturn(0L);

        assertThatThrownBy(() -> service.remove(MY_COMPANY, admin)).isInstanceOf(ResourceNotFoundException.class);

        verify(scoreHistoryRepository, never()).save(anyString(), any());
    }

    private AuthenticatedUser user(String email, String role) {
        return new AuthenticatedUser(UUID.randomUUID(), email, null, MY_COMPANY, role);
    }

    private ZSetOperations.TypedTuple<String> tuple(UUID companyId, double score) {
        return new DefaultTypedTuple<>(companyId.toString(), score);
    }
}
