package com.volta.ranking.dto;

import lombok.Data;

@Data
public class ScoreUpdateRequestDTO {

    private String companyUuid;

    private Double score;

    private String motivo;
}
