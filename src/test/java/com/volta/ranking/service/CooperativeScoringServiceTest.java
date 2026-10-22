package com.volta.ranking.service;

import com.volta.ranking.client.VoltaApiClient;
import com.volta.ranking.config.ScoringProperties;
import com.volta.ranking.dto.scoring.IncidentCooperativeScoresResponseDTO;
import com.volta.ranking.dto.voltaapi.AiReportDTO;
import com.volta.ranking.dto.voltaapi.CooperativeDataDTO;
import com.volta.ranking.dto.voltaapi.CoordinatesDTO;
import com.volta.ranking.dto.voltaapi.IncidentContextDTO;
import com.volta.ranking.dto.voltaapi.WasteTypeDTO;
import com.volta.ranking.exception.BusinessRuleException;
import com.volta.ranking.exception.ExternalServiceException;
import com.volta.ranking.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CooperativeScoringServiceTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final BigDecimal LATITUDE = new BigDecimal("-23.550520");
    private static final BigDecimal LONGITUDE = new BigDecimal("-46.633308");

    private final UUID incidentId = UUID.randomUUID();
    private final VoltaApiClient voltaApiClient = mock(VoltaApiClient.class);
    private final ScoringProperties properties = new ScoringProperties(
            new BigDecimal("100"),
            0.5,
            0.4,
            new ScoringProperties.Weights(0.6, 0.2, 0.2),
            new ScoringProperties.Weights(0.4, 0.3, 0.3));
    private final CooperativeScoringService service = new CooperativeScoringService(
            voltaApiClient, new CooperativeScoreCalculator(properties), properties);

    @Test
    void scoresTheCooperativesOfTheIncidentWithTheDefaultRadius() {
        AiReportDTO aiReport = new AiReportDTO("Óleo", "HIGH", "Isolar a área");
        when(voltaApiClient.getIncidentContext(incidentId, AUTHORIZATION))
                .thenReturn(incident("METAL", "LOW", aiReport));
        when(voltaApiClient.getCooperatives(AUTHORIZATION)).thenReturn(List.of(
                cooperative("Qualified", "METAL,PERIGOSO"),
                cooperative("General", "METAL"),
                cooperative("Other", "VIDRO")));

        IncidentCooperativeScoresResponseDTO response = service.scoreCooperatives(incidentId, null, 10, AUTHORIZATION);

        assertThat(response.incidentId()).isEqualTo(incidentId);
        assertThat(response.priority()).isEqualTo("HIGH");
        assertThat(response.wasteCategory()).isEqualTo("METAL");
        assertThat(response.highRisk()).isTrue();
        assertThat(response.radiusKm()).isEqualTo(100.0);
        assertThat(response.cooperatives()).extracting("name").containsExactly("Qualified", "General");
    }

    @Test
    void honorsTheRequestedRadiusAndLimit() {
        when(voltaApiClient.getIncidentContext(incidentId, AUTHORIZATION))
                .thenReturn(incident("METAL", "LOW", null));
        List<CooperativeDataDTO> cooperatives = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            cooperatives.add(cooperative("Coop " + index, "METAL"));
        }
        when(voltaApiClient.getCooperatives(AUTHORIZATION)).thenReturn(cooperatives);

        IncidentCooperativeScoresResponseDTO response =
                service.scoreCooperatives(incidentId, new BigDecimal("25"), 3, AUTHORIZATION);

        assertThat(response.radiusKm()).isEqualTo(25.0);
        assertThat(response.cooperatives()).hasSize(3);
    }

    @Test
    void returnsAnEmptyListWhenNoCooperativeIsEligible() {
        when(voltaApiClient.getIncidentContext(incidentId, AUTHORIZATION))
                .thenReturn(incident("METAL", "LOW", null));
        when(voltaApiClient.getCooperatives(AUTHORIZATION)).thenReturn(List.of());

        assertThat(service.scoreCooperatives(incidentId, null, 10, AUTHORIZATION).cooperatives()).isEmpty();
    }

    @Test
    void rejectsRadiusAndLimitOutOfRangeWithoutCallingTheVoltaApi() {
        assertThatThrownBy(() -> service.scoreCooperatives(incidentId, BigDecimal.ZERO, 10, AUTHORIZATION))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("Radius");
        assertThatThrownBy(() -> service.scoreCooperatives(incidentId, new BigDecimal("20001"), 10, AUTHORIZATION))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.scoreCooperatives(incidentId, null, 0, AUTHORIZATION))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("Limit");
        assertThatThrownBy(() -> service.scoreCooperatives(incidentId, null, 101, AUTHORIZATION))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(voltaApiClient);
    }

    @Test
    void rejectsAnIncidentWithoutOriginCoordinates() {
        IncidentContextDTO withoutOrigin = new IncidentContextDTO(
                incidentId, "LOW", new WasteTypeDTO("METAL", "LOW"), null, null, null);
        IncidentContextDTO withoutLongitude = new IncidentContextDTO(
                incidentId, "LOW", new WasteTypeDTO("METAL", "LOW"), null,
                new CoordinatesDTO(LATITUDE, null), null);

        when(voltaApiClient.getIncidentContext(incidentId, AUTHORIZATION)).thenReturn(withoutOrigin);
        assertThatThrownBy(() -> service.scoreCooperatives(incidentId, null, 10, AUTHORIZATION))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Company coordinates");

        when(voltaApiClient.getIncidentContext(incidentId, AUTHORIZATION)).thenReturn(withoutLongitude);
        assertThatThrownBy(() -> service.scoreCooperatives(incidentId, null, 10, AUTHORIZATION))
                .isInstanceOf(BusinessRuleException.class);

        verify(voltaApiClient, never()).getCooperatives(AUTHORIZATION);
    }

    @Test
    void rejectsRadioactiveWaste() {
        when(voltaApiClient.getIncidentContext(incidentId, AUTHORIZATION))
                .thenReturn(incident("RADIOATIVO", "HIGH", null));

        assertThatThrownBy(() -> service.scoreCooperatives(incidentId, null, 10, AUTHORIZATION))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Radioactive");

        verify(voltaApiClient, never()).getCooperatives(AUTHORIZATION);
    }

    @Test
    void treatsAnIncompleteIncidentAsAnInvalidVoltaApiResponse() {
        CoordinatesDTO origin = new CoordinatesDTO(LATITUDE, LONGITUDE);
        IncidentContextDTO[] invalid = {
                new IncidentContextDTO(incidentId, null, new WasteTypeDTO("METAL", "LOW"), null, origin, null),
                new IncidentContextDTO(incidentId, "LOW", null, null, origin, null),
                new IncidentContextDTO(incidentId, "LOW", new WasteTypeDTO(null, "LOW"), null, origin, null),
                new IncidentContextDTO(incidentId, "LOW", new WasteTypeDTO("METAL", null), null, origin, null)
        };

        for (IncidentContextDTO incident : invalid) {
            when(voltaApiClient.getIncidentContext(incidentId, AUTHORIZATION)).thenReturn(incident);

            assertThatThrownBy(() -> service.scoreCooperatives(incidentId, null, 10, AUTHORIZATION))
                    .isInstanceOf(ExternalServiceException.class)
                    .hasMessageContaining("priority or waste type is missing");
        }
    }

    private IncidentContextDTO incident(String category, String defaultRisk, AiReportDTO aiReport) {
        return new IncidentContextDTO(
                incidentId,
                "HIGH",
                new WasteTypeDTO(category, defaultRisk),
                null,
                new CoordinatesDTO(LATITUDE, LONGITUDE),
                aiReport);
    }

    private CooperativeDataDTO cooperative(String name, String specialties) {
        return new CooperativeDataDTO(
                UUID.randomUUID(), name, LATITUDE, LONGITUDE, new BigDecimal("4"), List.of(specialties.split(",")));
    }
}
