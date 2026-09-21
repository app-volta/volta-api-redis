package com.volta.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Credenciais para autenticação")
public class AuthRequestDTO {

    @NotBlank(message = "Username é obrigatório")
    @Schema(example = "gestor")
    @Size(max = 50, message = "Username deve ter no máximo 50 caracteres")
    private String username;

    @NotBlank(message = "Senha é obrigatória")
    @Schema(example = "senha123")
    @Size(max = 100, message = "Senha deve ter no máximo 100 caracteres")
    private String password;
}
