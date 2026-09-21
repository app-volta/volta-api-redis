package com.volta.ranking.controller;

import com.volta.ranking.dto.ApiResponseDTO;
import com.volta.ranking.dto.EmpresaRankingDTO;
import com.volta.ranking.dto.PosicaoResponseDTO;
import com.volta.ranking.dto.ScoreUpdateRequestDTO;
import com.volta.ranking.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
@Tag(name = "Ranking de Empresas", description = "Gerenciamento do ranking via Redis Sorted Set (ZSET)")
@SecurityRequirement(name = "bearerAuth")
public class RankingController {

    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private final RankingService rankingService;

    // ── GET: ranking completo ─────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    @Operation(
        summary = "Consultar ranking completo",
        description = "Retorna todas as empresas ordenadas da maior para menor pontuação.\n\n" +
                      "Redis: `ZREVRANGE ranking:companies 0 -1 WITHSCORES`"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ranking retornado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
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
    @Operation(
        summary = "Consultar top N empresas",
        description = "Retorna as N empresas com maior pontuação.\n\n" +
                      "Redis: `ZREVRANGE ranking:companies 0 {N-1} WITHSCORES`"
    )
    public ResponseEntity<ApiResponseDTO<List<EmpresaRankingDTO>>> topN(
        @Parameter(description = "Quantidade a retornar (1–100)", example = "10")
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "Limite deve ser no mínimo 1") @Max(value = 100, message = "Limite deve ser no máximo 100") int limit
    ) {
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Top " + limit + " empresas.",
            rankingService.consultarTopN(limit)
        ));
    }

    // ── GET: posição de uma empresa ───────────────────────────────────────────

    @GetMapping("/{companyUuid}/posicao")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    @Operation(
        summary = "Consultar posição de uma empresa",
        description = "Retorna posição, score e total de empresas no ranking.\n\n" +
                      "Redis: `ZREVRANK` + `ZSCORE` + `ZCARD`"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Posição encontrada"),
        @ApiResponse(responseCode = "404", description = "Empresa não está no ranking")
    })
    public ResponseEntity<ApiResponseDTO<PosicaoResponseDTO>> posicao(
        @Parameter(description = "UUID da empresa", example = "550e8400-e29b-41d4-a716-446655440000")
        @Pattern(regexp = UUID_REGEX, message = "UUID da empresa inválido") @PathVariable String companyUuid
    ) {
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Posição consultada com sucesso.",
            rankingService.consultarPosicao(companyUuid)
        ));
    }

    // ── GET: pontuação de uma empresa ─────────────────────────────────────────

    @GetMapping("/{companyUuid}/pontuacao")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    @Operation(
        summary = "Consultar pontuação de uma empresa",
        description = "Retorna apenas o score numérico.\n\nRedis: `ZSCORE ranking:companies \"{uuid}\"`"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pontuação retornada"),
        @ApiResponse(responseCode = "404", description = "Empresa não está no ranking")
    })
    public ResponseEntity<ApiResponseDTO<Double>> pontuacao(
        @Parameter(description = "UUID da empresa", example = "550e8400-e29b-41d4-a716-446655440000")
        @Pattern(regexp = UUID_REGEX, message = "UUID da empresa inválido") @PathVariable String companyUuid
    ) {
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Pontuação consultada.",
            rankingService.consultarPontuacao(companyUuid)
        ));
    }

    // ── GET: total ────────────────────────────────────────────────────────────

    @GetMapping("/total")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    @Operation(
        summary = "Total de empresas no ranking",
        description = "Redis: `ZCARD ranking:companies`"
    )
    public ResponseEntity<ApiResponseDTO<Long>> total() {
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Total de empresas no ranking.",
            rankingService.consultarTotal()
        ));
    }

    // ── POST: adicionar / atualizar score ─────────────────────────────────────

    @PostMapping("/score")
    @PreAuthorize("hasAnyRole('GESTOR','ADMIN')")
    @Operation(
        summary = "Adicionar ou atualizar pontuação",
        description = "Insere ou atualiza o score de uma empresa no ranking.\n\n" +
                      "**Roles:** GESTOR, ADMIN\n\n" +
                      "Redis: `ZADD ranking:companies {score} \"{uuid}\"`"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Score atualizado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Requer GESTOR ou ADMIN")
    })
    public ResponseEntity<ApiResponseDTO<Void>> atualizarScore(
        @Valid @RequestBody ScoreUpdateRequestDTO request
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
    @Operation(
        summary = "Remover empresa do ranking",
        description = "Remove a empresa do ranking (ex: inativação).\n\n" +
                      "**Role:** apenas ADMIN\n\n" +
                      "Redis: `ZREM ranking:companies \"{uuid}\"`"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Empresa removida"),
        @ApiResponse(responseCode = "403", description = "Requer ADMIN"),
        @ApiResponse(responseCode = "404", description = "Empresa não está no ranking")
    })
    public ResponseEntity<ApiResponseDTO<Void>> remover(
        @Parameter(description = "UUID da empresa", example = "550e8400-e29b-41d4-a716-446655440000")
        @Pattern(regexp = UUID_REGEX, message = "UUID da empresa inválido") @PathVariable String companyUuid
    ) {
        rankingService.removerDoRanking(companyUuid);
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Empresa " + companyUuid + " removida do ranking.",
            null
        ));
    }
}
