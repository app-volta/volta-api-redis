package com.volta.ranking.dto.voltaapi;

public record AiReportDTO(
        String detectedWasteType,
        String aiContaminationLevel,
        String recommendations
) {
}
