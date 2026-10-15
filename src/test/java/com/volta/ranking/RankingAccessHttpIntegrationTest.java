package com.volta.ranking;

import com.volta.ranking.config.LocalUsersConfiguration;
import com.volta.ranking.support.HttpTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class RankingAccessHttpIntegrationTest {

    private static final String MY_COMPANY = LocalUsersConfiguration.DEMO_COMPANY_ID.toString();

    private final String otherCompany = UUID.randomUUID().toString();
    private final String reason = "test-" + UUID.randomUUID();

    @Value("${local.server.port}")
    private int port;

    private HttpTestClient client;
    private String adminToken;

    @BeforeEach
    void setUp() {
        client = new HttpTestClient(port);
        adminToken = client.login("admin");
    }

    @AfterEach
    void cleanRanking() {
        client.delete("/api/v1/ranking/" + MY_COMPANY, adminToken);
        client.delete("/api/v1/ranking/" + otherCompany, adminToken);
    }

    @Test
    void employeeReadsOwnCompanyButNotOthers() {
        saveScore(adminToken, MY_COMPANY, 810, reason);
        saveScore(adminToken, otherCompany, 500, reason);
        String employeeToken = client.login("employee");

        for (String resource : new String[]{"position", "score", "history"}) {
            HttpResponse<String> own = client.get("/api/v1/ranking/" + MY_COMPANY + "/" + resource, employeeToken);
            HttpResponse<String> other = client.get("/api/v1/ranking/" + otherCompany + "/" + resource, employeeToken);

            assertThat(own.statusCode()).as("own company: %s", resource).isEqualTo(200);
            assertThat(other.statusCode()).as("other company: %s", resource).isEqualTo(404);
            assertThat(other.body()).contains("\"status\":404");
        }
    }

    @Test
    void adminReadsAnyCompany() {
        saveScore(adminToken, otherCompany, 500, reason);

        HttpResponse<String> response = client.get("/api/v1/ranking/" + otherCompany + "/position", adminToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"companyId\":\"" + otherCompany + "\"").contains("\"position\"");
    }

    @Test
    void managerUpdatesOnlyOwnCompany() {
        String managerToken = client.login("manager");

        assertThat(saveScore(managerToken, MY_COMPANY, 700, reason).statusCode()).isEqualTo(204);
        assertThat(saveScore(managerToken, otherCompany, 700, reason).statusCode()).isEqualTo(404);
        assertThat(client.get("/api/v1/ranking/" + otherCompany + "/score", adminToken).statusCode())
                .as("other company must not have been saved").isEqualTo(404);
    }

    @Test
    void historyRecordsScoreReasonAndAuthorNewestFirst() {
        String managerToken = client.login("manager");
        saveScore(managerToken, MY_COMPANY, 100, reason + "-old");
        saveScore(managerToken, MY_COMPANY, 200, reason + "-new");

        HttpResponse<String> response = client.get("/api/v1/ranking/" + MY_COMPANY + "/history?limit=2", managerToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"author\":\"manager@volta.com\"")
                .contains(reason + "-new")
                .contains(reason + "-old");
        assertThat(response.body().indexOf(reason + "-new")).isLessThan(response.body().indexOf(reason + "-old"));
    }

    @Test
    void removalStaysInHistory() {
        saveScore(adminToken, MY_COMPANY, 300, reason);

        assertThat(client.delete("/api/v1/ranking/" + MY_COMPANY, adminToken).statusCode()).isEqualTo(204);

        assertThat(client.get("/api/v1/ranking/" + MY_COMPANY + "/history?limit=1", adminToken).body())
                .contains("Removed from ranking");
        assertThat(client.delete("/api/v1/ranking/" + MY_COMPANY, adminToken).statusCode())
                .as("removing twice").isEqualTo(404);
    }

    @Test
    void paginationHonorsSizeAndPagePosition() {
        saveScore(adminToken, MY_COMPANY, 1000, reason);
        saveScore(adminToken, otherCompany, 999, reason);

        HttpResponse<String> first = client.get("/api/v1/ranking?page=0&size=1", adminToken);
        HttpResponse<String> second = client.get("/api/v1/ranking?page=1&size=1", adminToken);

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).contains("\"position\":1");
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.body()).contains("\"position\":2");
    }

    @Test
    void topAndTotalReflectTheRanking() {
        saveScore(adminToken, MY_COMPANY, 1000, reason);
        String employeeToken = client.login("employee");

        HttpResponse<String> top = client.get("/api/v1/ranking/top?limit=1", employeeToken);
        HttpResponse<String> total = client.get("/api/v1/ranking/total", employeeToken);

        assertThat(top.statusCode()).isEqualTo(200);
        assertThat(top.body()).contains("\"position\":1");
        assertThat(total.statusCode()).isEqualTo(200);
        assertThat(total.body()).contains("\"total\"");
    }

    @Test
    void batchLoadIsAdminOnlyAndSavesScores() {
        String batch = "{\"scores\":[{\"companyId\":\"" + MY_COMPANY + "\",\"score\":420},"
                + "{\"companyId\":\"" + otherCompany + "\",\"score\":310,\"reason\":\"" + reason + "\"}]}";

        assertThat(client.put("/api/v1/ranking/scores", client.login("manager"), batch).statusCode()).isEqualTo(403);

        HttpResponse<String> response = client.put("/api/v1/ranking/scores", adminToken, batch);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"saved\":2");
        assertThat(client.get("/api/v1/ranking/" + MY_COMPANY + "/score", adminToken).body()).contains("420");
        assertThat(client.get("/api/v1/ranking/" + otherCompany + "/history", adminToken).body()).contains(reason);
    }

    @Test
    void batchLoadRejectsInvalidBatches() {
        assertThat(client.put("/api/v1/ranking/scores", adminToken, "{\"scores\":[]}").statusCode()).isEqualTo(400);
        assertThat(client.put("/api/v1/ranking/scores", adminToken,
                "{\"scores\":[{\"companyId\":\"abc\",\"score\":1}]}").statusCode()).isEqualTo(400);
        assertThat(client.put("/api/v1/ranking/scores", adminToken,
                "{\"scores\":[{\"companyId\":\"" + MY_COMPANY + "\",\"score\":2000}]}").statusCode()).isEqualTo(400);
    }

    @Test
    void cooperativeLocationIsAdminOnlyAndSearchable() {
        String cooperativeId = UUID.randomUUID().toString();
        String location = "{\"latitude\":-23.550520,\"longitude\":-46.633308}";

        assertThat(client.put("/api/v1/cooperatives/" + cooperativeId + "/location",
                client.login("manager"), location).statusCode()).isEqualTo(403);
        assertThat(client.put("/api/v1/cooperatives/" + cooperativeId + "/location",
                adminToken, location).statusCode()).isEqualTo(204);

        HttpResponse<String> nearby = client.get(
                "/api/v1/cooperatives/nearby?latitude=-23.550520&longitude=-46.633308&radiusKm=5&limit=100",
                client.login("employee"));

        assertThat(nearby.statusCode()).isEqualTo(200);
        assertThat(nearby.body()).contains("\"cooperativeId\":\"" + cooperativeId + "\"").contains("\"distanceKm\"");

        assertThat(client.delete("/api/v1/cooperatives/" + cooperativeId + "/location", adminToken).statusCode())
                .isEqualTo(204);
        assertThat(client.delete("/api/v1/cooperatives/" + cooperativeId + "/location", adminToken).statusCode())
                .as("removing twice").isEqualTo(204);
    }

    private HttpResponse<String> saveScore(String token, String companyId, int score, String scoreReason) {
        String body = "{\"companyId\":\"" + companyId + "\",\"score\":" + score + ",\"reason\":\"" + scoreReason + "\"}";
        return client.post("/api/v1/ranking/score", token, body);
    }
}
