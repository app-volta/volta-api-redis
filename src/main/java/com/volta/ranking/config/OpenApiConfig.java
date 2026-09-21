package com.volta.ranking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "VOLTA Ranking API",
        version = "1.0.0",
        description = """
            API NoSQL responsável pelo Ranking de Empresas da plataforma VOLTA.

            Utiliza Redis Sorted Set (ZSET):
            - Key    → ranking:companies
            - Member → UUID da empresa
            - Score  → Pontuação (0–1000)

            **Roles:**
            | Role        | Permissões                          |
            |-------------|-------------------------------------|
            | FUNCIONARIO | Apenas leitura (GET)                |
            | GESTOR      | Leitura + atualizar scores (POST)   |
            | ADMIN       | Acesso total incluindo remoção      |

            Faça login em `/api/v1/auth/login` e cole o token no cadeado acima.
            """,
        contact = @Contact(name = "VOLTA Platform")
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT gerado pelo endpoint /api/v1/auth/login"
)
public class OpenApiConfig {
}
