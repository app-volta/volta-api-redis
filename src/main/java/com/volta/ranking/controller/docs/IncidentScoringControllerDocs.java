package com.volta.ranking.controller.docs;

import com.volta.ranking.dto.scoring.IncidentCooperativeScoresResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.UUID;

@Tag(name = "Score de cooperativas", description = "Calcula a aderência das cooperativas a uma ocorrência")
public interface IncidentScoringControllerDocs {

    @Operation(
            summary = "Calcula o score das cooperativas para uma ocorrência",
            description = "Busca na volta-api a ocorrência (com a análise de IA e a origem) e as cooperativas, "
                    + "e calcula aqui o score de 0 a 100 de cada cooperativa apta. Só entram cooperativas que "
                    + "atendem a categoria do resíduo, que têm a especialidade PERIGOSO quando o tipo de resíduo "
                    + "exige e que estão dentro do raio. O score combina proximidade, nota média e adequação ao "
                    + "risco; ocorrências de prioridade HIGH ou CRITICAL pesam mais a proximidade. O token do "
                    + "usuário é repassado à volta-api, que aplica o isolamento por empresa. "
                    + "**Acesso:** qualquer usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cooperativas ordenadas pelo maior score, possivelmente nenhuma"),
            @ApiResponse(responseCode = "400", description = "Raio ou limite inválidos"),
            @ApiResponse(responseCode = "404", description = "Ocorrência não encontrada na volta-api"),
            @ApiResponse(responseCode = "422", description = "Ocorrência sem coordenadas de origem ou com resíduo radioativo"),
            @ApiResponse(responseCode = "502", description = "volta-api indisponível ou com resposta inválida")
    })
    IncidentCooperativeScoresResponseDTO scoreCooperatives(
            @Parameter(description = "ID da ocorrência") UUID incidentId,
            @Parameter(description = "Raio de busca em quilômetros, maior que 0 e até 20000. Padrão: 100", example = "100")
            BigDecimal radiusKm,
            @Parameter(description = "Máximo de cooperativas, de 1 a 100", example = "10") int limit,
            @Parameter(hidden = true) String authorization
    );
}
