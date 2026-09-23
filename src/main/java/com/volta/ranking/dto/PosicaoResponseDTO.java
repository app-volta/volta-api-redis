package com.volta.ranking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PosicaoResponseDTO {

    private String companyUuid;

    private Long posicao;

    private Double score;

    private Long totalEmpresas;
}
