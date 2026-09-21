package com.volta.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Resposta de autenticação com token JWT")
public class AuthResponseDTO {

    @Schema(description = "Token JWT para uso nos endpoints protegidos")
    private String token;

    @Schema(example = "Bearer")
    private String tokenType;

    @Schema(example = "gestor")
    private String username;
}
