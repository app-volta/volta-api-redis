package com.volta.ranking.validation;

/**
 * Regras de validação reutilizadas em mais de um lugar (DTOs e parâmetros de rota).
 * Constantes de tempo de compilação, para poderem ser usadas dentro de anotações.
 */
public final class ValidationPatterns {

    /** UUID no formato 8-4-4-4-12, em hexadecimal (aceita maiúsculas e minúsculas). */
    public static final String UUID =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    public static final String UUID_MESSAGE = "UUID da empresa inválido";

    private ValidationPatterns() {
    }
}
