package com.volta.ranking.controller.docs;

import com.volta.ranking.dto.ranking.CompanyPositionResponseDTO;
import com.volta.ranking.dto.ranking.CompanyRankingResponseDTO;
import com.volta.ranking.dto.ranking.CompanyScoreResponseDTO;
import com.volta.ranking.dto.ranking.RankingTotalResponseDTO;
import com.volta.ranking.dto.ranking.ScoreBatchRequestDTO;
import com.volta.ranking.dto.ranking.ScoreBatchResponseDTO;
import com.volta.ranking.dto.ranking.ScoreHistoryResponseDTO;
import com.volta.ranking.dto.ranking.ScoreUpdateRequestDTO;
import com.volta.ranking.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "Ranking de empresas", description = "Pontuação e posição das empresas, mantidas em um Sorted Set do Redis")
public interface RankingControllerDocs {

    @Operation(
            summary = "Adiciona ou atualiza a pontuação de uma empresa",
            description = "Grava o score e registra a alteração no histórico. MANAGER só atualiza a própria empresa "
                    + "(outra empresa responde 404); ADMIN atualiza qualquer uma. "
                    + "**Acesso:** ADMIN e MANAGER. **Redis:** `ZADD`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pontuação gravada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "MANAGER tentando atualizar outra empresa")
    })
    void updateScore(ScoreUpdateRequestDTO dto, @Parameter(hidden = true) AuthenticatedUser author);

    @Operation(
            summary = "Carrega pontuações em lote",
            description = "Grava até 500 scores de uma vez. É o caminho para reconstruir o ranking depois de uma "
                    + "perda do Redis, já que o PostgreSQL da volta-api continua sendo a fonte da verdade. "
                    + "Empresas fora do lote não são alteradas nem removidas. "
                    + "**Acesso:** ADMIN. **Redis:** `ZADD` por item."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pontuações gravadas"),
            @ApiResponse(responseCode = "400", description = "Lote vazio, grande demais ou com item inválido")
    })
    ScoreBatchResponseDTO loadScores(ScoreBatchRequestDTO dto, @Parameter(hidden = true) AuthenticatedUser author);

    @Operation(
            summary = "Consulta o ranking",
            description = "Devolve uma página do ranking, da maior para a menor pontuação. A posição considera as "
                    + "páginas anteriores. **Acesso:** qualquer usuário autenticado. **Redis:** `ZREVRANGE`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página do ranking"),
            @ApiResponse(responseCode = "400", description = "Página ou tamanho fora da faixa")
    })
    List<CompanyRankingResponseDTO> showRanking(
            @Parameter(description = "Número da página, começando em 0", example = "0") int page,
            @Parameter(description = "Empresas por página, de 1 a 100", example = "50") int size
    );

    @Operation(
            summary = "Consulta as melhores empresas",
            description = "Devolve as N empresas com maior pontuação. "
                    + "**Acesso:** qualquer usuário autenticado. **Redis:** `ZREVRANGE`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Melhores empresas"),
            @ApiResponse(responseCode = "400", description = "Limite fora da faixa de 1 a 100")
    })
    List<CompanyRankingResponseDTO> showTop(
            @Parameter(description = "Quantidade de empresas, de 1 a 100", example = "10") int limit
    );

    @Operation(
            summary = "Consulta o total de empresas no ranking",
            description = "**Acesso:** qualquer usuário autenticado. **Redis:** `ZCARD`."
    )
    @ApiResponse(responseCode = "200", description = "Total de empresas")
    RankingTotalResponseDTO showTotal();

    @Operation(
            summary = "Consulta a posição de uma empresa",
            description = "Devolve posição, score e total de empresas. Quem não é ADMIN só consulta a própria "
                    + "empresa; outra empresa responde 404. "
                    + "**Acesso:** qualquer usuário autenticado. **Redis:** `ZREVRANK`, `ZSCORE` e `ZCARD`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição da empresa"),
            @ApiResponse(responseCode = "400", description = "ID da empresa inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa fora do ranking ou de outra empresa")
    })
    CompanyPositionResponseDTO showPosition(
            @Parameter(description = "ID da empresa") UUID companyId,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Consulta a pontuação de uma empresa",
            description = "Devolve apenas o score. Quem não é ADMIN só consulta a própria empresa; outra empresa "
                    + "responde 404. **Acesso:** qualquer usuário autenticado. **Redis:** `ZSCORE`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pontuação da empresa"),
            @ApiResponse(responseCode = "400", description = "ID da empresa inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa fora do ranking ou de outra empresa")
    })
    CompanyScoreResponseDTO showScore(
            @Parameter(description = "ID da empresa") UUID companyId,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Consulta o histórico de pontuação de uma empresa",
            description = "Últimas alterações de score (valor, motivo, autor e momento), da mais recente para a mais "
                    + "antiga. O Redis guarda até 100 alterações por empresa. Quem não é ADMIN só consulta a própria "
                    + "empresa; outra empresa responde 404. "
                    + "**Acesso:** qualquer usuário autenticado. **Redis:** `LRANGE`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico, vazio se não houver alterações"),
            @ApiResponse(responseCode = "400", description = "ID da empresa ou limite inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa de outra empresa")
    })
    List<ScoreHistoryResponseDTO> showHistory(
            @Parameter(description = "ID da empresa") UUID companyId,
            @Parameter(description = "Quantidade de alterações, de 1 a 100", example = "20") int limit,
            @Parameter(hidden = true) AuthenticatedUser author
    );

    @Operation(
            summary = "Remove uma empresa do ranking",
            description = "O histórico da empresa é mantido e registra a remoção. "
                    + "**Acesso:** ADMIN. **Redis:** `ZREM`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Empresa removida do ranking"),
            @ApiResponse(responseCode = "400", description = "ID da empresa inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa fora do ranking")
    })
    void remove(
            @Parameter(description = "ID da empresa") UUID companyId,
            @Parameter(hidden = true) AuthenticatedUser author
    );
}
