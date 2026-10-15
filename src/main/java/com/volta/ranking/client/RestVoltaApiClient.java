package com.volta.ranking.client;

import com.volta.ranking.dto.voltaapi.CooperativeDataDTO;
import com.volta.ranking.dto.voltaapi.IncidentContextDTO;
import com.volta.ranking.exception.ExternalServiceException;
import com.volta.ranking.exception.ResourceNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class RestVoltaApiClient implements VoltaApiClient {

    private final RestClient restClient;

    public RestVoltaApiClient(RestClient voltaApiRestClient) {
        this.restClient = voltaApiRestClient;
    }

    public IncidentContextDTO getIncidentContext(UUID incidentId, String authorization) {
        IncidentContextDTO context = execute("Incident", () -> restClient.get()
                .uri("/incidents/{id}/scoring-context", incidentId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(IncidentContextDTO.class));

        if (context == null) {
            throw new ExternalServiceException("Volta API");
        }

        return context;
    }

    public List<CooperativeDataDTO> getCooperatives(String authorization) {
        List<CooperativeDataDTO> cooperatives = execute("Cooperatives", () -> restClient.get()
                .uri("/cooperatives/scoring-data")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(new ParameterizedTypeReference<List<CooperativeDataDTO>>() {
                }));

        if (cooperatives == null) {
            throw new ExternalServiceException("Volta API");
        }

        return cooperatives;
    }

    private <T> T execute(String resource, Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(resource);
            }
            if (exception.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new AccessDeniedException("Volta API denied access to " + resource);
            }
            throw new ExternalServiceException("Volta API");
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ExternalServiceException("Volta API");
        }
    }
}
