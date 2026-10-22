package com.volta.ranking.controller.docs;

import com.volta.ranking.dto.cooperative.CooperativeLocationRequestDTO;
import com.volta.ranking.dto.cooperative.NearbyCooperativeResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Cooperativas", description = "Índice geoespacial de cooperativas, mantido no Redis GEO")
public interface CooperativeGeoControllerDocs {

    @Operation(
            summary = "Adiciona ou atualiza a localização de uma cooperativa",
            description = "O PostgreSQL da volta-api continua sendo a fonte da verdade; este índice é derivado. "
                    + "**Acesso:** ADMIN. **Redis:** `GEOADD`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Localização gravada"),
            @ApiResponse(responseCode = "400", description = "ID da cooperativa ou coordenadas inválidos")
    })
    void saveLocation(@Parameter(description = "ID da cooperativa") UUID cooperativeId, CooperativeLocationRequestDTO dto);

    @Operation(
            summary = "Remove a localização de uma cooperativa do índice",
            description = "Não falha se a cooperativa já não estiver no índice. "
                    + "**Acesso:** ADMIN. **Redis:** `ZREM` na chave GEO."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Localização removida"),
            @ApiResponse(responseCode = "400", description = "ID da cooperativa inválido")
    })
    void removeLocation(@Parameter(description = "ID da cooperativa") UUID cooperativeId);

    @Operation(
            summary = "Busca cooperativas próximas",
            description = "Devolve o ID e a distância em linha reta, em quilômetros, ordenados do mais próximo. "
                    + "A volta-api informa as coordenadas da empresa e completa os dados das cooperativas a partir "
                    + "do PostgreSQL. Um índice vazio devolve lista vazia. "
                    + "**Acesso:** qualquer usuário autenticado. **Redis:** `GEOSEARCH`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cooperativas encontradas, possivelmente nenhuma"),
            @ApiResponse(responseCode = "400", description = "Coordenadas, raio ou limite inválidos")
    })
    List<NearbyCooperativeResponseDTO> findNearby(
            @Parameter(description = "Latitude da empresa", example = "-23.550520") BigDecimal latitude,
            @Parameter(description = "Longitude da empresa", example = "-46.633308") BigDecimal longitude,
            @Parameter(description = "Raio da busca em quilômetros, maior que 0 e até 20000", example = "50")
            BigDecimal radiusKm,
            @Parameter(description = "Máximo de cooperativas, de 1 a 100", example = "20") int limit
    );
}
