package com.volta.ranking.dto.voltaapi;

import java.util.UUID;

public record IncidentContextDTO(
        UUID incidentId,
        String priority,
        WasteTypeDTO wasteType,
        String contaminationLevel,
        CoordinatesDTO origin,
        AiReportDTO aiReport
) {
}
