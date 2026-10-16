package com.volta.ranking.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestDTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void openValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void acceptsFilledCredentials() {
        assertThat(validate("admin@volta.com", "senha123")).isEmpty();
    }

    @Test
    void rejectsMissingOrBlankEmail() {
        assertThat(validate(null, "senha123")).extracting(ConstraintViolation::getMessage)
                .containsExactly("Email is required");
        assertThat(validate("  ", "senha123")).extracting(ConstraintViolation::getMessage)
                .contains("Email is required");
    }

    @Test
    void rejectsMalformedEmail() {
        assertThat(validate("not-an-email", "senha123")).extracting(ConstraintViolation::getMessage)
                .containsExactly("Email is invalid");
    }

    @Test
    void rejectsMissingOrBlankPassword() {
        assertThat(validate("admin@volta.com", null)).extracting(ConstraintViolation::getMessage)
                .containsExactly("Password is required");
        assertThat(validate("admin@volta.com", "")).extracting(ConstraintViolation::getMessage)
                .contains("Password is required");
    }

    @Test
    void rejectsFieldsThatAreTooLong() {
        String tooLongEmail = "a".repeat(60) + "@" + "b".repeat(50) + ".com";

        assertThat(validate(tooLongEmail, "senha123")).extracting(ConstraintViolation::getMessage)
                .contains("Email must have at most 100 characters");
        assertThat(validate("admin@volta.com", "x".repeat(100))).isEmpty();
        assertThat(validate("admin@volta.com", "x".repeat(101))).hasSize(1);
    }

    private Set<ConstraintViolation<LoginRequestDTO>> validate(String email, String password) {
        LoginRequestDTO dto = new LoginRequestDTO(email, password);
        return validator.validate(dto);
    }
}
