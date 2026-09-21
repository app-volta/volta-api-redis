package com.volta.ranking.service;

import com.volta.ranking.dto.EmpresaRankingDTO;
import com.volta.ranking.dto.PosicaoResponseDTO;
import com.volta.ranking.dto.ScoreUpdateRequestDTO;
import com.volta.ranking.exception.EmpresaNaoEncontradaException;
import com.volta.ranking.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingRepository rankingRepository;

    /**
     * Adiciona ou atualiza o score de uma empresa.
     * Redis: ZADD ranking:companies {score} "{uuid}"
     */
    public void adicionarOuAtualizarScore(ScoreUpdateRequestDTO request) {
        log.info("[RANKING] score {} → empresa {} | motivo: {}",
                request.getScore(), request.getCompanyUuid(), request.getMotivo());

        rankingRepository.adicionarOuAtualizar(request.getCompanyUuid(), request.getScore());

        log.info("[RANKING] score atualizado com sucesso para {}", request.getCompanyUuid());
    }

    /**
     * Ranking completo ordenado do maior para o menor score.
     * Redis: ZREVRANGE ranking:companies 0 -1 WITHSCORES
     */
    public List<EmpresaRankingDTO> consultarRankingCompleto() {
        return toDTO(rankingRepository.consultarRankingCompleto());
    }

    /**
     * Top N empresas com maior pontuação.
     * Redis: ZREVRANGE ranking:companies 0 {N-1} WITHSCORES
     */
    public List<EmpresaRankingDTO> consultarTopN(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("Limite deve ser maior que zero");
        return toDTO(rankingRepository.consultarTopN(limit));
    }

    /**
     * Posição, score e total de uma empresa específica.
     * Redis: ZREVRANK + ZSCORE + ZCARD
     */
    public PosicaoResponseDTO consultarPosicao(String companyUuid) {
        assertExiste(companyUuid);

        Long posicaoRedis = rankingRepository.consultarPosicao(companyUuid);
        Double score      = rankingRepository.consultarPontuacao(companyUuid);
        Long total        = rankingRepository.contarTotal();

        // Redis é 0-based (0 = 1º lugar), convertemos para 1-based
        long posicaoHumana = (posicaoRedis != null ? posicaoRedis : 0L) + 1;

        return PosicaoResponseDTO.builder()
                .companyUuid(companyUuid)
                .posicao(posicaoHumana)
                .score(score)
                .totalEmpresas(total)
                .build();
    }

    /**
     * Apenas o score numérico de uma empresa.
     * Redis: ZSCORE ranking:companies "{uuid}"
     */
    public Double consultarPontuacao(String companyUuid) {
        Double score = rankingRepository.consultarPontuacao(companyUuid);
        if (score == null) throw new EmpresaNaoEncontradaException(companyUuid);
        return score;
    }

    /**
     * Remove empresa do ranking (ex: inativação no sistema).
     * Redis: ZREM ranking:companies "{uuid}"
     */
    public void removerDoRanking(String companyUuid) {
        assertExiste(companyUuid);
        rankingRepository.remover(companyUuid);
        log.info("[RANKING] empresa {} removida do ranking", companyUuid);
    }

    /** Total de empresas no ranking. Redis: ZCARD */
    public Long consultarTotal() {
        return rankingRepository.contarTotal();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void assertExiste(String companyUuid) {
        if (!rankingRepository.existe(companyUuid)) {
            throw new EmpresaNaoEncontradaException(companyUuid);
        }
    }

    private List<EmpresaRankingDTO> toDTO(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<EmpresaRankingDTO> result = new ArrayList<>();
        if (tuples == null) return result;

        int posicao = 1;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            result.add(EmpresaRankingDTO.builder()
                    .posicao(posicao++)
                    .companyUuid(t.getValue())
                    .score(t.getScore())
                    .build());
        }
        return result;
    }
}
