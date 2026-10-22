package com.volta.ranking.security;

import com.volta.ranking.support.HttpTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DefaultProfileSecurityIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private HttpTestClient client;

    @BeforeEach
    void setUp() {
        client = new HttpTestClient(port);
    }

    @Test
    void demoLoginDoesNotExistOutsideTheLocalProfile() {
        HttpResponse<String> response = client.post("/api/v1/auth/login", null,
                "{\"email\":\"admin@volta.com\",\"password\":\"senha123\"}");

        assertThat(response.statusCode()).isIn(401, 403);
    }

    @Test
    void readinessProbeIsPublicAndRespondsWhenRedisIsAvailable() {
        HttpResponse<String> response = client.get("/actuator/health/readiness", null);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void swaggerIsNotAvailableOutsideTheLocalProfile() {
        HttpResponse<String> response = client.get("/api-docs", null);

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
