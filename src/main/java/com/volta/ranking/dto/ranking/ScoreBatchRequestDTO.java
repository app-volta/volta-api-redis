package com.volta.ranking.dto.ranking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Carga em lote de scores, usada para reconstruir o ranking depois de uma perda do Redis")
public record ScoreBatchRequestDTO(

        @Schema(description = "Scores a gravar, até " + ScoreBatchRequestDTO.MAX_SCORES + " por requisição")
        @NotEmpty(message = "At least one score is required")
        @Size(max = ScoreBatchRequestDTO.MAX_SCORES, message = "At most " + ScoreBatchRequestDTO.MAX_SCORES + " scores per request")
        @Valid
        List<ScoreUpdateRequestDTO> scores
) {

    public static final int MAX_SCORES = 500;
}
