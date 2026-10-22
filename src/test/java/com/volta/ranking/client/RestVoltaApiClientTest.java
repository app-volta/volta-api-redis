package com.volta.ranking.client;

import com.volta.ranking.dto.voltaapi.CooperativeDataDTO;
import com.volta.ranking.dto.voltaapi.IncidentContextDTO;
import com.volta.ranking.exception.ExternalServiceException;
import com.volta.ranking.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestVoltaApiClientTest {

    private static final String AUTHORIZATION = "Bearer user-token";

    private final UUID incidentId = UUID.randomUUID();
    private MockRestServiceServer server;
    private RestVoltaApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://volta-api");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestVoltaApiClient(builder.build());
    }

    @Test
    void readsTheIncidentContextAndForwardsTheUserToken() {
        String body = """
                {
                  "incidentId": "%s",
                  "priority": "CRITICAL",
                  "wasteType": {"category": "PERIGOSO", "defaultRiskLevel": "HIGH"},
                  "contaminationLevel": "MEDIUM",
                  "origin": {"latitude": -23.561684, "longitude": -46.655981},
                  "aiReport": {"detectedWasteType": "Óleo", "aiContaminationLevel": "HIGH", "recommendations": "Isolar"},
                  "unknownField": "ignored"
                }
                """.formatted(incidentId);
        server.expect(requestTo("http://volta-api/incidents/" + incidentId + "/scoring-context"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", AUTHORIZATION))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        IncidentContextDTO context = client.getIncidentContext(incidentId, AUTHORIZATION);

        assertThat(context.incidentId()).isEqualTo(incidentId);
        assertThat(context.priority()).isEqualTo("CRITICAL");
        assertThat(context.wasteType().category()).isEqualTo("PERIGOSO");
        assertThat(context.wasteType().defaultRiskLevel()).isEqualTo("HIGH");
        assertThat(context.contaminationLevel()).isEqualTo("MEDIUM");
        assertThat(context.origin().latitude()).isEqualByComparingTo("-23.561684");
        assertThat(context.aiReport().aiContaminationLevel()).isEqualTo("HIGH");
        server.verify();
    }

    @Test
    void readsAnIncidentContextWithoutOriginAndAiReport() {
        String body = """
                {
                  "incidentId": "%s",
                  "priority": "LOW",
                  "wasteType": {"category": "PLASTICO", "defaultRiskLevel": "LOW"},
                  "contaminationLevel": "LOW",
                  "origin": null,
                  "aiReport": null
                }
                """.formatted(incidentId);
        server.expect(requestTo("http://volta-api/incidents/" + incidentId + "/scoring-context"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        IncidentContextDTO context = client.getIncidentContext(incidentId, AUTHORIZATION);

        assertThat(context.origin()).isNull();
        assertThat(context.aiReport()).isNull();
        assertThat(context.wasteType().category()).isEqualTo("PLASTICO");
    }

    @Test
    void readsCooperativesWithEmptySpecialtiesAndWithoutRating() {
        String body = """
                [{"id": "%s", "name": "Sem especialidade", "latitude": -23.561684, "longitude": -46.655981,
                  "averageRating": null, "specialties": []}]
                """.formatted(UUID.randomUUID());
        server.expect(requestTo("http://volta-api/cooperatives/scoring-data"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<CooperativeDataDTO> cooperatives = client.getCooperatives(AUTHORIZATION);

        assertThat(cooperatives).singleElement().satisfies(cooperative -> {
            assertThat(cooperative.specialties()).isEmpty();
            assertThat(cooperative.averageRating()).isNull();
            assertThat(cooperative.latitude()).isEqualByComparingTo("-23.561684");
            assertThat(cooperative.longitude()).isEqualByComparingTo("-46.655981");
        });
    }

    @Test
    void readsTheCooperativeList() {
        String id = UUID.randomUUID().toString();
        String body = """
                [{"id": "%s", "name": "Recicla", "latitude": -23.5, "longitude": -46.6,
                  "averageRating": 4.5, "specialties": ["PAPEL", "PERIGOSO"]}]
                """.formatted(id);
        server.expect(requestTo("http://volta-api/cooperatives/scoring-data"))
                .andExpect(header("Authorization", AUTHORIZATION))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<CooperativeDataDTO> cooperatives = client.getCooperatives(AUTHORIZATION);

        assertThat(cooperatives).singleElement().satisfies(cooperative -> {
            assertThat(cooperative.id()).hasToString(id);
            assertThat(cooperative.name()).isEqualTo("Recicla");
            assertThat(cooperative.averageRating()).isEqualByComparingTo("4.5");
            assertThat(cooperative.specialties()).containsExactly("PAPEL", "PERIGOSO");
        });
    }

    @Test
    void mapsNotFoundToResourceNotFound() {
        server.expect(requestTo("http://volta-api/incidents/" + incidentId + "/scoring-context"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getIncidentContext(incidentId, AUTHORIZATION))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Incident not found");
    }

    @Test
    void mapsForbiddenToAccessDenied() {
        server.expect(requestTo("http://volta-api/cooperatives/scoring-data"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.getCooperatives(AUTHORIZATION))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void mapsOtherClientErrorsToExternalServiceFailure() {
        server.expect(requestTo("http://volta-api/cooperatives/scoring-data"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getCooperatives(AUTHORIZATION))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void mapsServerErrorsToExternalServiceFailure() {
        server.expect(requestTo("http://volta-api/cooperatives/scoring-data"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getCooperatives(AUTHORIZATION))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void mapsAnInvalidContractToExternalServiceFailure() {
        String body = "{\"incidentId\": \"not-a-uuid\", \"priority\": \"HIGH\"}";
        server.expect(requestTo("http://volta-api/incidents/" + incidentId + "/scoring-context"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getIncidentContext(incidentId, AUTHORIZATION))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void mapsAnEmptyBodyToExternalServiceFailure() {
        server.expect(requestTo("http://volta-api/incidents/" + incidentId + "/scoring-context"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://volta-api/cooperatives/scoring-data"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getIncidentContext(incidentId, AUTHORIZATION))
                .isInstanceOf(ExternalServiceException.class);
        assertThatThrownBy(() -> client.getCooperatives(AUTHORIZATION))
                .isInstanceOf(ExternalServiceException.class);
    }
}
