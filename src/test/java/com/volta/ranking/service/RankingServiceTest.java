package com.volta.ranking.service;

import com.volta.ranking.dto.EmpresaRankingDTO;
import com.volta.ranking.dto.PosicaoResponseDTO;
import com.volta.ranking.dto.ScoreUpdateRequestDTO;
import com.volta.ranking.exception.EmpresaNaoEncontradaException;
import com.volta.ranking.repository.RankingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testa a regra de negócio do ranking isolada do Redis (RankingRepository mockado).
 * A conexão real com o Redis é validada manualmente (ver descrição do PR).
 */
@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private RankingRepository rankingRepository;

    @InjectMocks
    private RankingService rankingService;

    private static final String UUID_EMPRESA = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    void adicionarOuAtualizarScore_delegaParaORepositorioComOsDadosDaRequisicao() {
        ScoreUpdateRequestDTO request = new ScoreUpdateRequestDTO();
        request.setCompanyUuid(UUID_EMPRESA);
        request.setScore(950.0);
        request.setMotivo("Coleta concluída");

        rankingService.adicionarOuAtualizarScore(request);

        verify(rankingRepository).adicionarOuAtualizar(UUID_EMPRESA, 950.0);
    }

    @Test
    void consultarTopN_comLimiteZero_lancaIllegalArgumentException() {
        assertThatThrownBy(() -> rankingService.consultarTopN(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior que zero");

        verifyNoInteractions(rankingRepository);
    }

    @Test
    void consultarTopN_comLimiteNegativo_lancaIllegalArgumentException() {
        assertThatThrownBy(() -> rankingService.consultarTopN(-5))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(rankingRepository);
    }

    @Test
    void consultarRankingCompleto_mantemAOrdemDoRedisEConvertePosicaoBaseUm() {
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(tupla("empresa-b", 900.0));
        tuples.add(tupla("empresa-a", 850.0));
        when(rankingRepository.consultarRankingCompleto()).thenReturn(tuples);

        List<EmpresaRankingDTO> ranking = rankingService.consultarRankingCompleto();

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).getCompanyUuid()).isEqualTo("empresa-b");
        assertThat(ranking.get(0).getPosicao()).isEqualTo(1);
        assertThat(ranking.get(1).getCompanyUuid()).isEqualTo("empresa-a");
        assertThat(ranking.get(1).getPosicao()).isEqualTo(2);
    }

    @Test
    void consultarRankingCompleto_quandoRedisRetornaNull_devolveListaVazia() {
        when(rankingRepository.consultarRankingCompleto()).thenReturn(null);

        assertThat(rankingService.consultarRankingCompleto()).isEmpty();
    }

    @Test
    void consultarPosicao_convertePosicaoZeroBasedDoRedisParaUmBased() {
        when(rankingRepository.existe(UUID_EMPRESA)).thenReturn(true);
        when(rankingRepository.consultarPosicao(UUID_EMPRESA)).thenReturn(0L); // 1º lugar no Redis
        when(rankingRepository.consultarPontuacao(UUID_EMPRESA)).thenReturn(920.0);
        when(rankingRepository.contarTotal()).thenReturn(3L);

        PosicaoResponseDTO posicao = rankingService.consultarPosicao(UUID_EMPRESA);

        assertThat(posicao.getPosicao()).isEqualTo(1L);
        assertThat(posicao.getScore()).isEqualTo(920.0);
        assertThat(posicao.getTotalEmpresas()).isEqualTo(3L);
    }

    @Test
    void consultarPosicao_empresaInexistente_lancaEmpresaNaoEncontrada() {
        when(rankingRepository.existe(UUID_EMPRESA)).thenReturn(false);

        assertThatThrownBy(() -> rankingService.consultarPosicao(UUID_EMPRESA))
                .isInstanceOf(EmpresaNaoEncontradaException.class)
                .hasMessageContaining(UUID_EMPRESA);

        verify(rankingRepository, never()).consultarPontuacao(anyString());
    }

    @Test
    void consultarPontuacao_empresaInexistente_lancaEmpresaNaoEncontrada() {
        when(rankingRepository.consultarPontuacao(UUID_EMPRESA)).thenReturn(null);

        assertThatThrownBy(() -> rankingService.consultarPontuacao(UUID_EMPRESA))
                .isInstanceOf(EmpresaNaoEncontradaException.class);
    }

    @Test
    void removerDoRanking_empresaExistente_chamaORemoverDoRepositorio() {
        when(rankingRepository.existe(UUID_EMPRESA)).thenReturn(true);

        rankingService.removerDoRanking(UUID_EMPRESA);

        verify(rankingRepository).remover(UUID_EMPRESA);
    }

    @Test
    void removerDoRanking_empresaInexistente_naoChamaORemoverEPropagaExcecao() {
        when(rankingRepository.existe(UUID_EMPRESA)).thenReturn(false);

        assertThatThrownBy(() -> rankingService.removerDoRanking(UUID_EMPRESA))
                .isInstanceOf(EmpresaNaoEncontradaException.class);

        verify(rankingRepository, never()).remover(anyString());
    }

    @Test
    void consultarTotal_delegaParaORepositorio() {
        when(rankingRepository.contarTotal()).thenReturn(7L);

        assertThat(rankingService.consultarTotal()).isEqualTo(7L);
    }

    private ZSetOperations.TypedTuple<String> tupla(String uuid, double score) {
        return new DefaultTypedTuple<>(uuid, score);
    }
}
