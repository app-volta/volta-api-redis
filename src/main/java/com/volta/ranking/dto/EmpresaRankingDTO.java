package com.volta.ranking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmpresaRankingDTO {

    private Integer posicao;

    private String companyUuid;

    private Double score;
}
