package com.volta.ranking;

import com.volta.ranking.config.LocalUsersConfiguration;
import com.volta.ranking.support.HttpTestClient;
import com.volta.ranking.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ErrorHandlingHttpIntegrationTest {

    private static final String UNKNOWN_COMPANY_ID = "660e8400-e29b-41d4-a716-446655440099";

    @Value("${local.server.port}")
    private int port;

    @Value("${volta.jwt.secret}")
    private String jwtSecret;

    private HttpTestClient client;

    @BeforeEach
    void setUp() {
        client = new HttpTestClient(port);
    }

    @Test
    void returnsProblemDetailWhenTokenIsMissing() {
        HttpResponse<String> response = client.get("/api/v1/ranking/total", null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertProblemDetail(response, 401, "/api/v1/ranking/total");
    }

    @Test
    void returnsUnauthorizedWhenTokenIsInvalid() {
        HttpResponse<String> response = client.get("/api/v1/ranking/total", "garbage.not.valid");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void acceptsTokenIssuedByVoltaApi() {
        String token = TestTokens.voltaApiToken(jwtSecret, "EMPLOYEE", LocalUsersConfiguration.DEMO_COMPANY_ID);

        HttpResponse<String> response = client.get(
                "/api/v1/cooperatives/nearby?latitude=90&longitude=0&radiusKm=10", token);

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsTokenWithUnknownRole() {
        String token = TestTokens.voltaApiToken(jwtSecret, "STRANGER", LocalUsersConfiguration.DEMO_COMPANY_ID);

        HttpResponse<String> response = client.get("/api/v1/ranking/total", token);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void returnsProblemDetailWhenRoleIsNotAllowed() {
        String token = client.login("employee");

        HttpResponse<String> response = client.post("/api/v1/ranking/score", token,
                "{\"companyId\":\"550e8400-e29b-41d4-a716-446655440000\",\"score\":100}");

        assertThat(response.statusCode()).isEqualTo(403);
        assertProblemDetail(response, 403, "/api/v1/ranking/score");
    }

    @Test
    void returnsUnauthorizedWhenPasswordIsWrong() {
        HttpResponse<String> response = client.post("/api/v1/auth/login", null,
                "{\"email\":\"admin@volta.com\",\"password\":\"wrong\"}");

        assertThat(response.statusCode()).isEqualTo(401);
        assertProblemDetail(response, 401, "/api/v1/auth/login");
    }

    @Test
    void returnsNotFoundForUnknownRoute() {
        String token = client.login("admin");

        HttpResponse<String> response = client.get("/api/v1/route/does-not-exist", token);

        assertThat(response.statusCode()).isEqualTo(404);
        assertProblemDetail(response, 404, "/api/v1/route/does-not-exist");
    }

    @Test
    void returnsMethodNotAllowedForUnsupportedMethod() {
        String token = client.login("admin");

        HttpResponse<String> response = client.put("/api/v1/ranking/score", token, "{}");

        assertThat(response.statusCode()).isEqualTo(405);
        assertProblemDetail(response, 405, "/api/v1/ranking/score");
    }

    @Test
    void returnsBadRequestWhenJsonIsMalformed() {
        String token = client.login("admin");

        HttpResponse<String> response = client.post("/api/v1/ranking/score", token, "{broken");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void returnsBadRequestWhenCompanyIdIsNotAnUuid() {
        String token = client.login("admin");

        HttpResponse<String> response = client.get("/api/v1/ranking/not-a-uuid/position", token);

        assertThat(response.statusCode()).isEqualTo(400);
        assertProblemDetail(response, 400, "/api/v1/ranking/not-a-uuid/position");
    }

    @Test
    void returnsNotFoundWhenCompanyIsNotInRanking() {
        String token = client.login("admin");

        HttpResponse<String> response = client.get("/api/v1/ranking/" + UNKNOWN_COMPANY_ID + "/position", token);

        assertThat(response.statusCode()).isEqualTo(404);
        assertProblemDetail(response, 404, "/api/v1/ranking/" + UNKNOWN_COMPANY_ID + "/position");
        assertThat(response.body()).contains("Ranking entry not found");
    }

    private void assertProblemDetail(HttpResponse<String> response, int status, String instance) {
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/problem+json");
        assertThat(response.body())
                .contains("\"status\":" + status)
                .contains("\"detail\"")
                .contains("\"instance\":\"" + instance + "\"");
    }
}
