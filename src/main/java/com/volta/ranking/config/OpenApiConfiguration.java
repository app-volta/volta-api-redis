package com.volta.ranking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Volta Ranking API",
                version = "v1",
                description = "API auxiliar do Volta para o ranking de empresas (Redis Sorted Set) e a busca de "
                        + "cooperativas por proximidade (Redis GEO). O PostgreSQL da volta-api continua sendo a "
                        + "fonte da verdade; este serviço guarda índices derivados. "
                        + "Use um JWT emitido pela volta-api no botão **Authorize**. O login de demonstração "
                        + "`POST /api/v1/auth/login` existe somente no perfil local.\n\n"
                        + "**Perfis:** ADMIN administra o ranking e o índice GEO; MANAGER consulta e atualiza a "
                        + "pontuação da própria empresa; EMPLOYEE apenas consulta. Quem não é ADMIN só acessa os "
                        + "dados da própria empresa."
        ),
        security = @SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
)
@SecurityScheme(
        name = OpenApiConfiguration.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT emitido pela volta-api"
)
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";

    private static final String PROBLEM_DETAIL = "ProblemDetail";

    @Bean
    public OpenApiCustomizer globalResponsesCustomizer() {
        return openApi -> {
            openApi.getComponents().addSchemas(PROBLEM_DETAIL, problemDetailSchema());
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(this::addDefaultResponses)
            );
        };
    }

    private void addDefaultResponses(Operation operation) {
        ApiResponses responses = operation.getResponses();

        if (isSecured(operation)) {
            responses.putIfAbsent("401", new ApiResponse().description("Token ausente, inválido ou expirado"));
            responses.putIfAbsent("403", new ApiResponse().description("Usuário sem permissão para acessar este recurso"));
        }
        responses.putIfAbsent("500", new ApiResponse().description("Erro interno do servidor"));

        responses.forEach((code, response) -> {
            if (code.startsWith("4") || code.startsWith("5")) {
                response.setContent(problemDetailContent());
            }
        });
    }

    private boolean isSecured(Operation operation) {
        return operation.getSecurity() == null || !operation.getSecurity().isEmpty();
    }

    private Content problemDetailContent() {
        MediaType mediaType = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_DETAIL));
        return new Content().addMediaType("application/problem+json", mediaType);
    }

    private Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .description("Formato padrão de erro da API (RFC 9457)")
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().example("Not Found"))
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("detail", new StringSchema().example("Ranking entry not found"))
                .addProperty("instance", new StringSchema().example("/api/v1/ranking/550e8400-e29b-41d4-a716-446655440000/position"))
                .addProperty("errors", new ObjectSchema()
                        .description("Erros por campo ou parâmetro. Presente apenas em erros de validação (400)")
                        .additionalProperties(new StringSchema())
                        .example(Map.of("score", "Score must be at most 1000")));
    }
}
