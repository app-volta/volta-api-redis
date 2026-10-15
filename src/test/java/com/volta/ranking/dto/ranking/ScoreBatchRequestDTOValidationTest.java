package com.volta.ranking.dto.ranking;

import com.volta.ranking.dto.ranking.score.ScoreBatchRequestDTO;
import com.volta.ranking.dto.ranking.score.ScoreUpdateRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreBatchRequestDTOValidationTest {

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
    void acceptsBatchWithinTheLimit() {
        assertThat(validate(buildScores(1))).isEmpty();
        assertThat(validate(buildScores(ScoreBatchRequestDTO.MAX_SCORES))).isEmpty();
    }

    @Test
    void rejectsMissingOrEmptyBatch() {
        assertThat(validate(null)).hasSize(1);
        assertThat(validate(List.of())).hasSize(1);
    }

    @Test
    void rejectsBatchAboveTheLimit() {
        assertThat(validate(buildScores(ScoreBatchRequestDTO.MAX_SCORES + 1))).hasSize(1);
    }

    @Test
    void validatesEachItem() {
        List<ScoreUpdateRequestDTO> scores = List.of(new ScoreUpdateRequestDTO(UUID.randomUUID(), 2000.0, null));

        assertThat(validate(scores)).hasSize(1);
    }

    private List<ScoreUpdateRequestDTO> buildScores(int amount) {
        List<ScoreUpdateRequestDTO> scores = new ArrayList<>();

        for (int index = 0; index < amount; index++) {
            scores.add(new ScoreUpdateRequestDTO(UUID.randomUUID(), 100.0, null));
        }

        return scores;
    }

    private Set<ConstraintViolation<ScoreBatchRequestDTO>> validate(List<ScoreUpdateRequestDTO> scores) {
        ScoreBatchRequestDTO dto = new ScoreBatchRequestDTO(scores);
        return validator.validate(dto);
    }
}
