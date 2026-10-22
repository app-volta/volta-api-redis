package com.volta.ranking.client;

import com.volta.ranking.dto.voltaapi.CooperativeDataDTO;
import com.volta.ranking.dto.voltaapi.IncidentContextDTO;

import java.util.List;
import java.util.UUID;

public interface VoltaApiClient {

    IncidentContextDTO getIncidentContext(UUID incidentId, String authorization);

    List<CooperativeDataDTO> getCooperatives(String authorization);
}
