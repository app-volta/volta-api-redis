package com.volta.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Credenciais para autenticação")
public class AuthRequestDTO {

    @Schema(example = "gestor")
    private String username;

    @Schema(example = "senha123")
    private String password;
}
