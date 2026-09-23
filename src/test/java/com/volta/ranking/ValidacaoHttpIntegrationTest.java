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
 * Sobe a aplicação de verdade (Tomcat em porta aleatória) e confere o status HTTP devolvido
 * para entradas inválidas.
 *
 * Precisa ser um teste de integração real: o 403 indevido que este teste protege só acontecia
 * no encaminhamento do container para /error, etapa que o MockMvc não executa.
 *
 * Não precisa de Redis: toda requisição inválida é recusada antes de chegar ao serviço.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "JWT_KEY=chave-de-teste-com-mais-de-256-bits-para-o-algoritmo-hs512-1234567890"
)
class ValidacaoHttpIntegrationTest {

    private static final String UUID_VALIDO = "550e8400-e29b-41d4-a716-446655440000";
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @Value("${local.server.port}")
    private int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void scoreAcimaDoLimite_retorna400() throws Exception {
        String token = login("admin", "senha123");

        assertThat(post("/api/v1/ranking/score", token,
                "{\"companyUuid\":\"" + UUID_VALIDO + "\",\"score\":5000}").statusCode()).isEqualTo(400);
    }

    @Test
    void scoreAusente_retorna400() throws Exception {
        String token = login("admin", "senha123");

        assertThat(post("/api/v1/ranking/score", token,
                "{\"companyUuid\":\"" + UUID_VALIDO + "\"}").statusCode()).isEqualTo(400);
    }

    @Test
    void uuidInvalidoNoCorpo_retorna400() throws Exception {
        String token = login("admin", "senha123");

        assertThat(post("/api/v1/ranking/score", token,
                "{\"companyUuid\":\"abc\",\"score\":10}").statusCode()).isEqualTo(400);
    }

    @Test
    void uuidInvalidoNaRota_retorna400() throws Exception {
        String token = login("admin", "senha123");

        assertThat(get("/api/v1/ranking/abc/posicao", token).statusCode()).isEqualTo(400);
        assertThat(get("/api/v1/ranking/abc/pontuacao", token).statusCode()).isEqualTo(400);
        assertThat(delete("/api/v1/ranking/abc", token).statusCode()).isEqualTo(400);
    }

    @Test
    void limitForaDaFaixa_retorna400() throws Exception {
        String token = login("admin", "senha123");

        assertThat(get("/api/v1/ranking/top?limit=0", token).statusCode()).isEqualTo(400);
        assertThat(get("/api/v1/ranking/top?limit=101", token).statusCode()).isEqualTo(400);
    }

    @Test
    void loginSemSenha_retorna400() throws Exception {
        assertThat(post("/api/v1/auth/login", null, "{\"username\":\"admin\"}").statusCode()).isEqualTo(400);
    }

    @Test
    void autorizacaoVemAntesDaValidacao_funcionarioComCorpoInvalidoRecebe403() throws Exception {
        String token = login("funcionario", "senha123");

        assertThat(post("/api/v1/ranking/score", token,
                "{\"companyUuid\":\"abc\",\"score\":5000}").statusCode()).isEqualTo(403);
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

    private HttpResponse<String> delete(String caminho, String token) throws IOException, InterruptedException {
        return enviar(requisicao(caminho, token).DELETE().build());
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
