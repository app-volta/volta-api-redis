package com.volta.ranking.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa as regras de validação do corpo de POST /ranking/score,
 * sem subir o Spring: só o Bean Validation.
 */
class ScoreUpdateRequestDTOValidationTest {

    private static final String UUID_VALIDO = "550e8400-e29b-41d4-a716-446655440000";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void abrirValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 500.0, 999.99, 1000.0})
    void scoreDentroDaFaixa_ehValido(double score) {
        assertThat(validar(UUID_VALIDO, score, "motivo")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -1.0, 1000.01, 5000.0})
    void scoreForaDaFaixa_ehInvalido(double score) {
        Set<ConstraintViolation<ScoreUpdateRequestDTO>> erros = validar(UUID_VALIDO, score, null);

        assertThat(erros).hasSize(1);
        assertThat(erros.iterator().next().getPropertyPath()).hasToString("score");
    }

    @Test
    void scoreAusente_ehInvalido() {
        Set<ConstraintViolation<ScoreUpdateRequestDTO>> erros = validar(UUID_VALIDO, null, null);

        assertThat(erros).extracting(ConstraintViolation::getMessage)
                .containsExactly("A pontuação é obrigatória");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "550e8400-e29b-41d4-a716", "550e8400e29b41d4a716446655440000",
            "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz", "550e8400-e29b-41d4-a716-446655440000-extra"})
    void uuidMalFormado_ehInvalido(String uuid) {
        Set<ConstraintViolation<ScoreUpdateRequestDTO>> erros = validar(uuid, 100.0, null);

        assertThat(erros).extracting(ConstraintViolation::getMessage)
                .containsExactly("UUID da empresa inválido");
    }

    @ParameterizedTest
    @ValueSource(strings = {"550e8400-e29b-41d4-a716-446655440000", "550E8400-E29B-41D4-A716-446655440000"})
    void uuidBemFormado_aceitaMaiusculasEMinusculas(String uuid) {
        assertThat(validar(uuid, 100.0, null)).isEmpty();
    }

    @Test
    void uuidNuloOuEmBranco_ehInvalido() {
        assertThat(validar(null, 100.0, null)).isNotEmpty();
        assertThat(validar("   ", 100.0, null)).isNotEmpty();
    }

    @Test
    void motivoComMaisDe255Caracteres_ehInvalido() {
        assertThat(validar(UUID_VALIDO, 100.0, "a".repeat(255))).isEmpty();
        assertThat(validar(UUID_VALIDO, 100.0, "a".repeat(256))).hasSize(1);
    }

    private Set<ConstraintViolation<ScoreUpdateRequestDTO>> validar(String uuid, Double score, String motivo) {
        ScoreUpdateRequestDTO dto = new ScoreUpdateRequestDTO();
        dto.setCompanyUuid(uuid);
        dto.setScore(score);
        dto.setMotivo(motivo);
        return validator.validate(dto);
    }
}
