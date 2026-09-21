package com.volta.ranking.controller;

import com.volta.ranking.dto.ApiResponseDTO;
import com.volta.ranking.dto.EmpresaRankingDTO;
import com.volta.ranking.dto.PosicaoResponseDTO;
import com.volta.ranking.dto.ScoreUpdateRequestDTO;
import com.volta.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    // ── GET: ranking completo ─────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<EmpresaRankingDTO>>> rankingCompleto() {
        List<EmpresaRankingDTO> ranking = rankingService.consultarRankingCompleto();
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Ranking retornado. Total: " + ranking.size() + " empresa(s).",
            ranking
        ));
    }

    // ── GET: top N ────────────────────────────────────────────────────────────

    @GetMapping("/top")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<EmpresaRankingDTO>>> topN(
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Top " + limit + " empresas.",
            rankingService.consultarTopN(limit)
        ));
    }

    // ── GET: posição de uma empresa ───────────────────────────────────────────

    @GetMapping("/{companyUuid}/posicao")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    public ResponseEntity<ApiResponseDTO<PosicaoResponseDTO>> posicao(
         @PathVariable String companyUuid
    ) {
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Posição consultada com sucesso.",
            rankingService.consultarPosicao(companyUuid)
        ));
    }

    // ── GET: pontuação de uma empresa ─────────────────────────────────────────

    @GetMapping("/{companyUuid}/pontuacao")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    public ResponseEntity<ApiResponseDTO<Double>> pontuacao(
         @PathVariable String companyUuid
    ) {
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Pontuação consultada.",
            rankingService.consultarPontuacao(companyUuid)
        ));
    }

    // ── GET: total ────────────────────────────────────────────────────────────

    @GetMapping("/total")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    public ResponseEntity<ApiResponseDTO<Long>> total() {
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Total de empresas no ranking.",
            rankingService.consultarTotal()
        ));
    }

    // ── POST: adicionar / atualizar score ─────────────────────────────────────

    @PostMapping("/score")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> atualizarScore(
         @RequestBody ScoreUpdateRequestDTO request
    ) {
        rankingService.adicionarOuAtualizarScore(request);
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Score da empresa " + request.getCompanyUuid() +
            " atualizado para " + request.getScore() + ".",
            null
        ));
    }

    // ── DELETE: remover empresa ───────────────────────────────────────────────

    @DeleteMapping("/{companyUuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> remover(
         @PathVariable String companyUuid
    ) {
        rankingService.removerDoRanking(companyUuid);
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Empresa " + companyUuid + " removida do ranking.",
            null
        ));
    }
}
