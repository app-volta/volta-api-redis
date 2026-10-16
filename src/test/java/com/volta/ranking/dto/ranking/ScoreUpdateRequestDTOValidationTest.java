package com.volta.ranking.dto.ranking;

import com.volta.ranking.dto.ranking.score.ScoreUpdateRequestDTO;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreUpdateRequestDTOValidationTest {

    private static final UUID COMPANY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

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

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 500.0, 999.99, 1000.0})
    void acceptsScoreWithinRange(double score) {
        assertThat(validate(COMPANY_ID, score, "reason")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -1.0, 1000.01, 5000.0})
    void rejectsScoreOutOfRange(double score) {
        Set<ConstraintViolation<ScoreUpdateRequestDTO>> errors = validate(COMPANY_ID, score, null);

        assertThat(errors).hasSize(1);
        assertThat(errors.iterator().next().getPropertyPath()).hasToString("score");
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNotANumberAndInfinity(double score) {
        assertThat(validate(COMPANY_ID, score, null)).isNotEmpty();
    }

    @Test
    void rejectsMissingScore() {
        Set<ConstraintViolation<ScoreUpdateRequestDTO>> errors = validate(COMPANY_ID, null, null);

        assertThat(errors).extracting(ConstraintViolation::getMessage).containsExactly("Score is required");
    }

    @Test
    void rejectsMissingCompanyId() {
        Set<ConstraintViolation<ScoreUpdateRequestDTO>> errors = validate(null, 100.0, null);

        assertThat(errors).extracting(ConstraintViolation::getMessage).containsExactly("Company id is required");
    }

    @Test
    void rejectsReasonLongerThan255Characters() {
        assertThat(validate(COMPANY_ID, 100.0, "a".repeat(255))).isEmpty();
        assertThat(validate(COMPANY_ID, 100.0, "a".repeat(256))).hasSize(1);
    }

    private Set<ConstraintViolation<ScoreUpdateRequestDTO>> validate(UUID companyId, Double score, String reason) {
        ScoreUpdateRequestDTO dto = new ScoreUpdateRequestDTO(companyId, score, reason);
        return validator.validate(dto);
    }
}
