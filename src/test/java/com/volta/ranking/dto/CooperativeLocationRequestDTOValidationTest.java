package com.volta.ranking.dto;

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
        var request = new CooperativeLocationRequestDTO(
                new BigDecimal("-85.051128"), new BigDecimal("180.000000"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsCoordinatesOutsideBoundsOrWithExcessPrecision() {
        assertThat(validator.validate(new CooperativeLocationRequestDTO(
                new BigDecimal("85.051129"), BigDecimal.ZERO))).isNotEmpty();
        assertThat(validator.validate(new CooperativeLocationRequestDTO(
                BigDecimal.ZERO, new BigDecimal("180.000001")))).isNotEmpty();
        assertThat(validator.validate(new CooperativeLocationRequestDTO(
                new BigDecimal("1.1234567"), BigDecimal.ZERO))).isNotEmpty();
    }
}
