package com.volta.ranking;

import com.volta.ranking.support.FakeVoltaApi;
import com.volta.ranking.support.HttpTestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class IncidentScoringHttpIntegrationTest {

    private static final FakeVoltaApi VOLTA_API = new FakeVoltaApi();

    private static final String NEAR_QUALIFIED = "11111111-1111-4111-8111-111111111111";
    private static final String FAR_QUALIFIED = "22222222-2222-4222-8222-222222222222";
    private static final String PLASTIC_ONLY = "33333333-3333-4333-8333-333333333333";

    private final String incidentId = UUID.randomUUID().toString();
    private final String contextPath = "/incidents/" + incidentId + "/scoring-context";
    private final String cooperativesPath = "/cooperatives/scoring-data";

    @Value("${local.server.port}")
    private int port;

    private HttpTestClient client;

    @DynamicPropertySource
    static void voltaApiProperties(DynamicPropertyRegistry registry) {
        registry.add("volta.api.base-url", VOLTA_API::baseUrl);
    }

    @AfterAll
    static void stopVoltaApi() {
        VOLTA_API.close();
    }

    @BeforeEach
    void setUp() {
        client = new HttpTestClient(port);
        VOLTA_API.reset();
        VOLTA_API.respond(cooperativesPath, 200, cooperatives());
    }

    @Test
    void scoresTheCooperativesUsingTheIncidentAiAnalysisAndTheDistance() {
        VOLTA_API.respond(contextPath, 200, context("PERIGOSO", "HIGH", "CRITICAL", origin(), aiReport("HIGH")));
        String token = client.login("employee");

        HttpResponse<String> response = client.get("/api/v1/incidents/" + incidentId + "/cooperative-scores", token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"incidentId\":\"" + incidentId + "\"")
                .contains("\"priority\":\"CRITICAL\"")
                .contains("\"wasteCategory\":\"PERIGOSO\"")
                .contains("\"highRisk\":true")
                .contains("\"cooperativeId\":\"" + NEAR_QUALIFIED + "\"")
                .contains("\"cooperativeId\":\"" + FAR_QUALIFIED + "\"")
                .doesNotContain(PLASTIC_ONLY);
        assertThat(response.body().indexOf(NEAR_QUALIFIED)).isLessThan(response.body().indexOf(FAR_QUALIFIED));
        assertThat(VOLTA_API.receivedAuthorization(contextPath)).isEqualTo("Bearer " + token);
        assertThat(VOLTA_API.receivedAuthorization(cooperativesPath)).isEqualTo("Bearer " + token);
    }

    @Test
    void honorsRadiusAndLimit() {
        VOLTA_API.respond(contextPath, 200, context("PERIGOSO", "HIGH", "HIGH", origin(), aiReport("HIGH")));
        String token = client.login("manager");

        HttpResponse<String> limited = client.get(
                "/api/v1/incidents/" + incidentId + "/cooperative-scores?limit=1", token);
        HttpResponse<String> narrow = client.get(
                "/api/v1/incidents/" + incidentId + "/cooperative-scores?radiusKm=5", token);

        assertThat(limited.body()).contains(NEAR_QUALIFIED).doesNotContain(FAR_QUALIFIED);
        assertThat(narrow.body()).contains(NEAR_QUALIFIED).doesNotContain(FAR_QUALIFIED);
        assertThat(narrow.body()).contains("\"radiusKm\":5.0");
    }

    @Test
    void returnsNotFoundWhenTheVoltaApiDoesNotKnowTheIncident() {
        HttpResponse<String> response = client.get(
                "/api/v1/incidents/" + incidentId + "/cooperative-scores", client.login("employee"));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Incident not found");
    }

    @Test
    void returnsBadGatewayWhenTheVoltaApiFails() {
        VOLTA_API.respond(contextPath, 500, "{}");

        HttpResponse<String> response = client.get(
                "/api/v1/incidents/" + incidentId + "/cooperative-scores", client.login("employee"));

        assertThat(response.statusCode()).isEqualTo(502);
    }

    @Test
    void returnsUnprocessableWhenTheIncidentHasNoOrigin() {
        VOLTA_API.respond(contextPath, 200, context("PLASTICO", "LOW", "LOW", null, null));

        HttpResponse<String> response = client.get(
                "/api/v1/incidents/" + incidentId + "/cooperative-scores", client.login("employee"));

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("Company coordinates");
    }

    @Test
    void returnsUnprocessableForRadioactiveWaste() {
        VOLTA_API.respond(contextPath, 200, context("RADIOATIVO", "HIGH", "CRITICAL", origin(), null));

        HttpResponse<String> response = client.get(
                "/api/v1/incidents/" + incidentId + "/cooperative-scores", client.login("employee"));

        assertThat(response.statusCode()).isEqualTo(422);
    }

    @Test
    void rejectsInvalidRadiusAndLimit() {
        String token = client.login("employee");
        String path = "/api/v1/incidents/" + incidentId + "/cooperative-scores";

        assertThat(client.get(path + "?radiusKm=0", token).statusCode()).isEqualTo(400);
        assertThat(client.get(path + "?radiusKm=20001", token).statusCode()).isEqualTo(400);
        assertThat(client.get(path + "?limit=0", token).statusCode()).isEqualTo(400);
        assertThat(client.get(path + "?limit=101", token).statusCode()).isEqualTo(400);
        assertThat(client.get("/api/v1/incidents/not-a-uuid/cooperative-scores", token).statusCode()).isEqualTo(400);
    }

    @Test
    void requiresAuthentication() {
        HttpResponse<String> response = client.get("/api/v1/incidents/" + incidentId + "/cooperative-scores", null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private String context(String category, String defaultRisk, String priority, String origin, String aiReport) {
        return """
                {
                  "incidentId": "%s",
                  "priority": "%s",
                  "wasteType": {"category": "%s", "defaultRiskLevel": "%s"},
                  "contaminationLevel": "MEDIUM",
                  "origin": %s,
                  "aiReport": %s
                }
                """.formatted(incidentId, priority, category, defaultRisk, origin, aiReport);
    }

    private String origin() {
        return "{\"latitude\": -23.550520, \"longitude\": -46.633308}";
    }

    private String aiReport(String level) {
        return "{\"detectedWasteType\": \"Óleo lubrificante\", \"aiContaminationLevel\": \""
                + level + "\", \"recommendations\": \"Isolar a área\"}";
    }

    private String cooperatives() {
        return """
                [
                  {"id": "%s", "name": "Near", "latitude": -23.560000, "longitude": -46.640000,
                   "averageRating": 4.5, "specialties": ["PERIGOSO", "METAL"]},
                  {"id": "%s", "name": "Far", "latitude": -23.100000, "longitude": -46.633308,
                   "averageRating": 5.0, "specialties": ["PERIGOSO"]},
                  {"id": "%s", "name": "Plastic", "latitude": -23.550520, "longitude": -46.633308,
                   "averageRating": 5.0, "specialties": ["PLASTICO"]}
                ]
                """.formatted(NEAR_QUALIFIED, FAR_QUALIFIED, PLASTIC_ONLY);
    }
}
