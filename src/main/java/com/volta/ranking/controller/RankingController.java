package com.volta.ranking.controller;

import com.volta.ranking.controller.docs.RankingControllerDocs;
import com.volta.ranking.dto.ranking.company.CompanyPositionResponseDTO;
import com.volta.ranking.dto.ranking.company.CompanyRankingResponseDTO;
import com.volta.ranking.dto.ranking.company.CompanyScoreResponseDTO;
import com.volta.ranking.dto.ranking.RankingTotalResponseDTO;
import com.volta.ranking.dto.ranking.score.ScoreBatchRequestDTO;
import com.volta.ranking.dto.ranking.score.ScoreBatchResponseDTO;
import com.volta.ranking.dto.ranking.score.ScoreHistoryResponseDTO;
import com.volta.ranking.dto.ranking.score.ScoreUpdateRequestDTO;
import com.volta.ranking.security.AuthenticatedUser;
import com.volta.ranking.usecase.RankingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ranking")
public class RankingController implements RankingControllerDocs {

    private final RankingUseCase rankingUseCase;

    @PostMapping("/score")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateScore(
            @Valid @RequestBody ScoreUpdateRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        rankingUseCase.updateScore(dto, author);
    }

    @PutMapping("/scores")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ScoreBatchResponseDTO loadScores(
            @Valid @RequestBody ScoreBatchRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        ScoreBatchResponseDTO response = rankingUseCase.loadScores(dto, author);
        return response;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OPERATOR')")
    public List<CompanyRankingResponseDTO> showRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        List<CompanyRankingResponseDTO> ranking = rankingUseCase.getRanking(page, size);
        return ranking;
    }

    @GetMapping("/top")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OPERATOR')")
    public List<CompanyRankingResponseDTO> showTop(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<CompanyRankingResponseDTO> top = rankingUseCase.getTop(limit);
        return top;
    }

    @GetMapping("/total")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OPERATOR')")
    public RankingTotalResponseDTO showTotal() {
        RankingTotalResponseDTO total = rankingUseCase.getTotal();
        return total;
    }

    @GetMapping("/{companyId}/position")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OPERATOR')")
    public CompanyPositionResponseDTO showPosition(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        CompanyPositionResponseDTO position = rankingUseCase.getPosition(companyId, author);
        return position;
    }

    @GetMapping("/{companyId}/score")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OPERATOR')")
    public CompanyScoreResponseDTO showScore(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        CompanyScoreResponseDTO score = rankingUseCase.getScore(companyId, author);
        return score;
    }

    @GetMapping("/{companyId}/history")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OPERATOR')")
    public List<ScoreHistoryResponseDTO> showHistory(
            @PathVariable UUID companyId,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        List<ScoreHistoryResponseDTO> history = rankingUseCase.getHistory(companyId, limit, author);
        return history;
    }

    @DeleteMapping("/{companyId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser author
    ) {
        rankingUseCase.remove(companyId, author);
    }
}
