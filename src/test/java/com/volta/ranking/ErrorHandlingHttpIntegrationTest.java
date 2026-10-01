package com.volta.ranking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe a aplicação de verdade (Tomcat em porta aleatória) para testar o tratamento de erros
 * ponta a ponta. Precisa ser um teste de integração real pelo mesmo motivo do
 * ValidacaoHttpIntegrationTest: a cadeia de filtros do Spring Security
 * (AuthorizationFilter → ExceptionTranslationFilter → RestAuthHandler) não é exercitada
 * pelo MockMvc.
 *
 * Não precisa de Redis: nenhum destes casos chega a consultar o ranking.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "JWT_KEY=chave-de-teste-com-mais-de-256-bits-para-o-algoritmo-hs512-1234567890"
)
class ErrorHandlingHttpIntegrationTest {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @Value("${local.server.port}")
    private int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void semToken_retorna401ComEnvelopePadrao() throws Exception {
        HttpResponse<String> resposta = get("/api/v1/ranking/total", null);

        assertThat(resposta.statusCode()).isEqualTo(401);
        assertThat(resposta.body()).contains("\"success\":false").contains("\"timestamp\"");
    }

    @Test
    void tokenInvalido_retorna401() throws Exception {
        assertThat(get("/api/v1/ranking/total", "lixo.nao.valido").statusCode()).isEqualTo(401);
    }

    @Test
    void autenticadoSemOPapel_retorna403ComEnvelopePadrao() throws Exception {
        // Regressão: antes da correção, este caminho (filtro, não o GlobalExceptionHandler)
        // devolvia um JSON escrito à mão, sem o campo "timestamp" que o resto da API tem.
        String token = login("funcionario", "senha123");

        HttpResponse<String> resposta = post("/api/v1/ranking/score", token,
                "{\"companyUuid\":\"550e8400-e29b-41d4-a716-446655440000\",\"score\":100}");

        assertThat(resposta.statusCode()).isEqualTo(403);
        assertThat(resposta.body()).contains("\"success\":false").contains("\"timestamp\"");
    }

    @Test
    void senhaErrada_retorna401() throws Exception {
        assertThat(post("/api/v1/auth/login", null,
                "{\"username\":\"admin\",\"password\":\"errada\"}").statusCode()).isEqualTo(401);
    }

    @Test
    void rotaInexistente_retorna404() throws Exception {
        // Dois segmentos que não casam com nenhum template mapeado (nem {companyUuid}/posicao,
        // nem {companyUuid}/pontuacao, nem {companyUuid} sozinho) — diferente de uma rota que
        // bate no template de outro verbo, o que resultaria em 405, não 404.
        String token = login("admin", "senha123");

        assertThat(get("/api/v1/rota/completamente-inexistente", token).statusCode()).isEqualTo(404);
    }

    @Test
    void metodoNaoSuportado_retorna405() throws Exception {
        String token = login("admin", "senha123");

        assertThat(enviar(requisicao("/api/v1/ranking/score", token)
                .header("Content-Type", "application/json")
                .PUT(BodyPublishers.ofString("{}"))
                .build()).statusCode()).isEqualTo(405);
    }

    @Test
    void jsonMalformado_retorna400() throws Exception {
        String token = login("admin", "senha123");

        assertThat(post("/api/v1/ranking/score", token, "{quebrado").statusCode()).isEqualTo(400);
    }

    @Test
    void empresaNaoEncontrada_retorna404DeNegocio() throws Exception {
        String token = login("admin", "senha123");

        HttpResponse<String> resposta = get(
                "/api/v1/ranking/660e8400-e29b-41d4-a716-446655440099/posicao", token);

        assertThat(resposta.statusCode()).isEqualTo(404);
        assertThat(resposta.body()).contains("não encontrada no ranking");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String login(String usuario, String senha) throws Exception {
        HttpResponse<String> resposta = post("/api/v1/auth/login", null,
                "{\"username\":\"" + usuario + "\",\"password\":\"" + senha + "\"}");
        assertThat(resposta.statusCode()).as("login de %s", usuario).isEqualTo(200);

        Matcher m = TOKEN.matcher(resposta.body());
        assertThat(m.find()).as("token na resposta do login").isTrue();
        return m.group(1);
    }

    private HttpResponse<String> get(String caminho, String token) throws IOException, InterruptedException {
        return enviar(requisicao(caminho, token).GET().build());
    }

    private HttpResponse<String> post(String caminho, String token, String json) throws IOException, InterruptedException {
        return enviar(requisicao(caminho, token)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(json))
                .build());
    }

    private HttpRequest.Builder requisicao(String caminho, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private HttpResponse<String> enviar(HttpRequest requisicao) throws IOException, InterruptedException {
        return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
