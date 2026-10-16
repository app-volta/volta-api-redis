package com.volta.ranking.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso")
public record TokenResponseDTO(

        @Schema(description = "Token JWT para o header Authorization: Bearer", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Validade do token, em milissegundos", example = "86400000")
        long expiresIn
) {
}
