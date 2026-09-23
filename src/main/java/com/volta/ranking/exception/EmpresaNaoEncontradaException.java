package com.volta.ranking.exception;

public class EmpresaNaoEncontradaException extends RuntimeException {

    public EmpresaNaoEncontradaException(String companyUuid) {
        super("Empresa com UUID '" + companyUuid + "' não encontrada no ranking.");
    }
}
