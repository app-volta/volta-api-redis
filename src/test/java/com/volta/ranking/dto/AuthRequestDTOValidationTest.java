package com.volta.ranking.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras de validação do corpo de POST /auth/login. */
class AuthRequestDTOValidationTest {

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

    @Test
    void credenciaisPreenchidas_saoValidas() {
        assertThat(validar("admin", "senha123")).isEmpty();
    }

    @Test
    void usernameAusenteOuEmBranco_ehInvalido() {
        assertThat(validar(null, "senha123")).extracting(ConstraintViolation::getMessage)
                .containsExactly("Username é obrigatório");
        assertThat(validar("  ", "senha123")).extracting(ConstraintViolation::getMessage)
                .contains("Username é obrigatório");
    }

    @Test
    void senhaAusenteOuEmBranco_ehInvalida() {
        assertThat(validar("admin", null)).extracting(ConstraintViolation::getMessage)
                .containsExactly("Senha é obrigatória");
        assertThat(validar("admin", "")).extracting(ConstraintViolation::getMessage)
                .contains("Senha é obrigatória");
    }

    @Test
    void camposMuitoLongos_saoInvalidos() {
        assertThat(validar("a".repeat(50), "senha123")).isEmpty();
        assertThat(validar("a".repeat(51), "senha123")).hasSize(1);
        assertThat(validar("admin", "x".repeat(100))).isEmpty();
        assertThat(validar("admin", "x".repeat(101))).hasSize(1);
    }

    private Set<ConstraintViolation<AuthRequestDTO>> validar(String username, String password) {
        AuthRequestDTO dto = new AuthRequestDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return validator.validate(dto);
    }
}
