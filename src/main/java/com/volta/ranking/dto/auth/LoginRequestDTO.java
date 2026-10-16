package com.volta.ranking.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais de login")
public record LoginRequestDTO(

        @Schema(description = "E-mail do usuário de demonstração", example = "manager@volta.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        @Size(max = 100, message = "Email must have at most 100 characters")
        String email,

        @Schema(description = "Senha do usuário de demonstração", example = "senha123")
        @NotBlank(message = "Password is required")
        @Size(max = 100, message = "Password must have at most 100 characters")
        String password
) {
}
