package com.volta.ranking;

import com.volta.ranking.support.HttpTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class ValidationHttpIntegrationTest {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Value("${local.server.port}")
    private int port;

    private HttpTestClient client;
    private String adminToken;

    @BeforeEach
    void setUp() {
        client = new HttpTestClient(port);
        adminToken = client.login("admin");
    }

    @Test
    void rejectsScoreAboveLimitWithErrorsByField() {
        HttpResponse<String> response = client.post("/api/v1/ranking/score", adminToken,
                "{\"companyId\":\"" + VALID_UUID + "\",\"score\":5000}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/problem+json");
        assertThat(response.body())
                .contains("\"status\":400")
                .contains("\"errors\"")
                .contains("\"score\":\"Score must be at most 1000\"");
    }

    @Test
    void rejectsMissingScore() {
        HttpResponse<String> response = client.post("/api/v1/ranking/score", adminToken,
                "{\"companyId\":\"" + VALID_UUID + "\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"score\":\"Score is required\"");
    }

    @Test
    void rejectsMalformedCompanyIdInBody() {
        HttpResponse<String> response = client.post("/api/v1/ranking/score", adminToken,
                "{\"companyId\":\"abc\",\"score\":10}");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsMalformedCompanyIdInPath() {
        assertThat(client.get("/api/v1/ranking/abc/position", adminToken).statusCode()).isEqualTo(400);
        assertThat(client.get("/api/v1/ranking/abc/score", adminToken).statusCode()).isEqualTo(400);
        assertThat(client.get("/api/v1/ranking/abc/history", adminToken).statusCode()).isEqualTo(400);
        assertThat(client.delete("/api/v1/ranking/abc", adminToken).statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsLimitOutOfRange() {
        HttpResponse<String> response = client.get("/api/v1/ranking/top?limit=0", adminToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("\"status\":400")
                .contains("Limit must be between 1 and 100");
        assertThat(client.get("/api/v1/ranking/top?limit=101", adminToken).statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsPageAndSizeOutOfRange() {
        assertThat(client.get("/api/v1/ranking?size=0", adminToken).statusCode()).isEqualTo(400);
        assertThat(client.get("/api/v1/ranking?size=101", adminToken).statusCode()).isEqualTo(400);
        assertThat(client.get("/api/v1/ranking?page=-1", adminToken).statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsNearbySearchOutOfRange() {
        String base = "/api/v1/cooperatives/nearby?latitude=-23.55&longitude=-46.63";

        assertThat(client.get(base + "&radiusKm=0", adminToken).statusCode()).isEqualTo(400);
        assertThat(client.get(base + "&radiusKm=20001", adminToken).statusCode()).isEqualTo(400);
        assertThat(client.get(base + "&radiusKm=10&limit=101", adminToken).statusCode()).isEqualTo(400);
        assertThat(client.get("/api/v1/cooperatives/nearby?latitude=86&longitude=0&radiusKm=10", adminToken)
                .statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsInvalidCooperativeLocation() {
        HttpResponse<String> response = client.put("/api/v1/cooperatives/" + VALID_UUID + "/location", adminToken,
                "{\"latitude\":91,\"longitude\":0}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"latitude\"");
    }

    @Test
    void rejectsLoginWithoutPassword() {
        HttpResponse<String> response = client.post("/api/v1/auth/login", null, "{\"email\":\"admin@volta.com\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"password\":\"Password is required\"");
    }

    @Test
    void checksAuthorizationBeforeValidation() {
        String employeeToken = client.login("employee");

        HttpResponse<String> response = client.post("/api/v1/ranking/score", employeeToken,
                "{\"companyId\":\"abc\",\"score\":5000}");

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
