package com.volta.ranking.dto.cooperative;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CooperativeLocationRequestDTOValidationTest {

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
    void acceptsCoordinatesWithinRedisGeoBounds() {
        CooperativeLocationRequestDTO dto = new CooperativeLocationRequestDTO(
                new BigDecimal("-85.051128"), new BigDecimal("180.000000"));

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void rejectsCoordinatesOutsideBoundsOrWithExcessPrecision() {
        CooperativeLocationRequestDTO latitudeTooHigh = new CooperativeLocationRequestDTO(
                new BigDecimal("85.051129"), BigDecimal.ZERO);
        CooperativeLocationRequestDTO longitudeTooHigh = new CooperativeLocationRequestDTO(
                BigDecimal.ZERO, new BigDecimal("180.000001"));
        CooperativeLocationRequestDTO tooManyDecimals = new CooperativeLocationRequestDTO(
                new BigDecimal("1.1234567"), BigDecimal.ZERO);

        assertThat(validator.validate(latitudeTooHigh)).isNotEmpty();
        assertThat(validator.validate(longitudeTooHigh)).isNotEmpty();
        assertThat(validator.validate(tooManyDecimals)).isNotEmpty();
    }

    @Test
    void rejectsMissingCoordinates() {
        CooperativeLocationRequestDTO dto = new CooperativeLocationRequestDTO(null, null);

        assertThat(validator.validate(dto)).hasSize(2);
    }
}
