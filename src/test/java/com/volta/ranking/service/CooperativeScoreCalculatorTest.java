package com.volta.ranking.service;

import com.volta.ranking.config.ScoringProperties;
import com.volta.ranking.dto.scoring.CooperativeScoreResponseDTO;
import com.volta.ranking.dto.voltaapi.AiReportDTO;
import com.volta.ranking.dto.voltaapi.CooperativeDataDTO;
import com.volta.ranking.dto.voltaapi.CoordinatesDTO;
import com.volta.ranking.dto.voltaapi.IncidentContextDTO;
import com.volta.ranking.dto.voltaapi.WasteTypeDTO;
import com.volta.ranking.utils.GeoDistance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CooperativeScoreCalculatorTest {

    private static final BigDecimal ORIGIN_LATITUDE = new BigDecimal("-23.550520");
    private static final BigDecimal ORIGIN_LONGITUDE = new BigDecimal("-46.633308");
    private static final BigDecimal FIFTY_KM_NORTH_LATITUDE = new BigDecimal("-23.100000");

    private final ScoringProperties properties = new ScoringProperties(
            new BigDecimal("100"),
            0.5,
            0.4,
            new ScoringProperties.Weights(0.6, 0.2, 0.2),
            new ScoringProperties.Weights(0.4, 0.3, 0.3));

    private final CooperativeScoreCalculator calculator = new CooperativeScoreCalculator(properties);

    @Test
    void givesFullScoreToATopRatedCooperativeRightAtTheOrigin() {
        IncidentContextDTO incident = incident("MEDIUM", "PLASTICO", "LOW", null, null);
        CooperativeDataDTO cooperative = cooperative("Recicla", ORIGIN_LATITUDE, "5.00", "PLASTICO");

        List<CooperativeScoreResponseDTO> scores = calculator.score(incident, List.of(cooperative), 100);

        assertThat(scores).singleElement().satisfies(score -> {
            assertThat(score.score()).isEqualTo(100.0);
            assertThat(score.distanceKm()).isZero();
            assertThat(score.breakdown().distance()).isEqualTo(100.0);
            assertThat(score.breakdown().rating()).isEqualTo(100.0);
            assertThat(score.breakdown().fit()).isEqualTo(100.0);
        });
    }

    @Test
    void combinesDistanceRatingAndFitWithTheStandardWeights() {
        IncidentContextDTO incident = incident("MEDIUM", "PLASTICO", "LOW", null, null);
        CooperativeDataDTO cooperative = cooperative("Recicla", FIFTY_KM_NORTH_LATITUDE, "4.00", "PLASTICO");
        double distance = distanceTo(cooperative);

        CooperativeScoreResponseDTO score = calculator.score(incident, List.of(cooperative), 100).get(0);

        double expected = 100 * (0.4 * (1 - distance / 100) + 0.3 * 0.8 + 0.3 * 1.0);
        assertThat(score.score()).isCloseTo(expected, within(0.01));
        assertThat(score.distanceKm()).isCloseTo(distance, within(0.01));
        assertThat(score.breakdown().rating()).isEqualTo(80.0);
    }

    @Test
    void weighsDistanceMoreForUrgentPriorities() {
        CooperativeDataDTO cooperative = cooperative("Recicla", FIFTY_KM_NORTH_LATITUDE, "4.00", "PLASTICO");
        double distance = distanceTo(cooperative);

        for (String priority : new String[]{"HIGH", "CRITICAL"}) {
            IncidentContextDTO incident = incident(priority, "PLASTICO", "LOW", null, null);

            CooperativeScoreResponseDTO score = calculator.score(incident, List.of(cooperative), 100).get(0);

            double expected = 100 * (0.6 * (1 - distance / 100) + 0.2 * 0.8 + 0.2 * 1.0);
            assertThat(score.score()).as("priority %s", priority).isCloseTo(expected, within(0.01));
        }
    }

    @Test
    void usesANeutralRatingForUnratedCooperatives() {
        IncidentContextDTO incident = incident("MEDIUM", "PLASTICO", "LOW", null, null);
        CooperativeDataDTO zero = cooperative("Zero", ORIGIN_LATITUDE, "0.00", "PLASTICO");
        CooperativeDataDTO missing = cooperative("Missing", ORIGIN_LATITUDE, null, "PLASTICO");

        List<CooperativeScoreResponseDTO> scores = calculator.score(incident, List.of(zero, missing), 100);

        assertThat(scores).hasSize(2);
        assertThat(scores).allSatisfy(score -> assertThat(score.breakdown().rating()).isEqualTo(50.0));
    }

    @Test
    void capsTheRatingAtFive() {
        IncidentContextDTO incident = incident("MEDIUM", "PLASTICO", "LOW", null, null);
        CooperativeDataDTO cooperative = cooperative("Recicla", ORIGIN_LATITUDE, "7.00", "PLASTICO");

        CooperativeScoreResponseDTO score = calculator.score(incident, List.of(cooperative), 100).get(0);

        assertThat(score.breakdown().rating()).isEqualTo(100.0);
    }

    @Test
    void skipsCooperativesThatDoNotHandleTheWasteCategory() {
        IncidentContextDTO incident = incident("MEDIUM", "VIDRO", "LOW", null, null);
        CooperativeDataDTO plastic = cooperative("Plastic", ORIGIN_LATITUDE, "5", "PLASTICO,PAPEL");
        CooperativeDataDTO glass = cooperative("Glass", ORIGIN_LATITUDE, "5", "VIDRO");
        CooperativeDataDTO none = cooperative("None", ORIGIN_LATITUDE, "5", null);

        List<CooperativeScoreResponseDTO> scores = calculator.score(incident, List.of(plastic, glass, none), 100);

        assertThat(scores).extracting(CooperativeScoreResponseDTO::name).containsExactly("Glass");
    }

    @Test
    void doesNotNormalizeACategoryOutsideTheContract() {
        IncidentContextDTO incident = incident("MEDIUM", "Não reciclável", "LOW", null, null);
        CooperativeDataDTO cooperative = cooperative("Recicla", ORIGIN_LATITUDE, "5.00", "NAO_RECICLAVEL");

        assertThat(calculator.score(incident, List.of(cooperative), 100)).isEmpty();
    }

    @Test
    void skipsCooperativesWithEmptySpecialties() {
        IncidentContextDTO incident = incident("MEDIUM", "PLASTICO", "LOW", null, null);
        CooperativeDataDTO cooperative = new CooperativeDataDTO(
                UUID.randomUUID(), "Vazia", ORIGIN_LATITUDE, ORIGIN_LONGITUDE, new BigDecimal("5"), List.of());

        assertThat(calculator.score(incident, List.of(cooperative), 100)).isEmpty();
    }

    @Test
    void requiresTheHazardousSpecialtyWhenTheWasteTypeIsHighRisk() {
        IncidentContextDTO incident = incident("HIGH", "PERIGOSO", "HIGH", null, null);
        CooperativeDataDTO qualified = cooperative("Qualified", ORIGIN_LATITUDE, "3", "PERIGOSO,METAL");
        CooperativeDataDTO unqualified = cooperative("Unqualified", ORIGIN_LATITUDE, "5", "SAUDE");

        List<CooperativeScoreResponseDTO> scores = calculator.score(incident, List.of(qualified, unqualified), 100);

        assertThat(scores).extracting(CooperativeScoreResponseDTO::name).containsExactly("Qualified");
    }

    @Test
    void penalizesWithoutExcludingWhenOnlyTheAiRaisesTheRisk() {
        AiReportDTO aiReport = new AiReportDTO("Óleo", "HIGH", "Isolar a área");
        IncidentContextDTO incident = incident("MEDIUM", "METAL", "LOW", null, aiReport);
        CooperativeDataDTO general = cooperative("General", ORIGIN_LATITUDE, "5", "METAL");
        CooperativeDataDTO qualified = cooperative("Qualified", ORIGIN_LATITUDE, "5", "METAL,PERIGOSO");

        List<CooperativeScoreResponseDTO> scores = calculator.score(incident, List.of(general, qualified), 100);

        assertThat(scores).extracting(CooperativeScoreResponseDTO::name).containsExactly("Qualified", "General");
        assertThat(scores.get(0).breakdown().fit()).isEqualTo(100.0);
        assertThat(scores.get(1).breakdown().fit()).isEqualTo(40.0);
        assertThat(scores.get(0).score()).isGreaterThan(scores.get(1).score());
    }

    @Test
    void flagsHighRiskWhenAnySourceSaysHigh() {
        AiReportDTO aiHigh = new AiReportDTO("x", "high", null);
        AiReportDTO aiMedium = new AiReportDTO("x", "MEDIUM", null);
        AiReportDTO aiUnknown = new AiReportDTO("x", "severe", null);

        assertThat(calculator.isHighRisk(incident("LOW", "METAL", "LOW", null, null))).isFalse();
        assertThat(calculator.isHighRisk(incident("LOW", "METAL", "LOW", null, aiMedium))).isFalse();
        assertThat(calculator.isHighRisk(incident("LOW", "METAL", "LOW", null, aiUnknown))).isFalse();
        assertThat(calculator.isHighRisk(incident("LOW", "METAL", "LOW", null, aiHigh))).isTrue();
        assertThat(calculator.isHighRisk(incident("LOW", "METAL", "LOW", "HIGH", aiMedium))).isTrue();
        assertThat(calculator.isHighRisk(incident("LOW", "METAL", "HIGH", null, null))).isTrue();
    }

    @Test
    void skipsCooperativesBeyondTheRadius() {
        IncidentContextDTO incident = incident("MEDIUM", "PLASTICO", "LOW", null, null);
        CooperativeDataDTO near = cooperative("Near", ORIGIN_LATITUDE, "5", "PLASTICO");
        CooperativeDataDTO far = cooperative("Far", FIFTY_KM_NORTH_LATITUDE, "5", "PLASTICO");

        assertThat(calculator.score(incident, List.of(near, far), 40))
                .extracting(CooperativeScoreResponseDTO::name).containsExactly("Near");
        assertThat(calculator.score(incident, List.of(near, far), 60)).hasSize(2);
    }

    @Test
    void skipsCooperativesWithoutCoordinates() {
        IncidentContextDTO incident = incident("MEDIUM", "PLASTICO", "LOW", null, null);
        CooperativeDataDTO withoutLatitude = new CooperativeDataDTO(
                UUID.randomUUID(), "NoLatitude", null, ORIGIN_LONGITUDE, new BigDecimal("5"), List.of("PLASTICO"));
        CooperativeDataDTO withoutLongitude = new CooperativeDataDTO(
                UUID.randomUUID(), "NoLongitude", ORIGIN_LATITUDE, null, new BigDecimal("5"), List.of("PLASTICO"));

        assertThat(calculator.score(incident, List.of(withoutLatitude, withoutLongitude), 100)).isEmpty();
    }

    @Test
    void ordersByScoreThenDistanceThenName() {
        IncidentContextDTO incident = incident("MEDIUM", "PLASTICO", "LOW", null, null);
        CooperativeDataDTO best = cooperative("Best", ORIGIN_LATITUDE, "5", "PLASTICO");
        CooperativeDataDTO tiedB = cooperative("b-tied", FIFTY_KM_NORTH_LATITUDE, "5", "PLASTICO");
        CooperativeDataDTO tiedA = cooperative("A-tied", FIFTY_KM_NORTH_LATITUDE, "5", "PLASTICO");
        CooperativeDataDTO worse = cooperative("Worse", FIFTY_KM_NORTH_LATITUDE, "1", "PLASTICO");

        List<CooperativeScoreResponseDTO> scores =
                calculator.score(incident, List.of(worse, tiedB, tiedA, best), 100);

        assertThat(scores).extracting(CooperativeScoreResponseDTO::name)
                .containsExactly("Best", "A-tied", "b-tied", "Worse");
    }

    @Test
    void ordersUnnamedCooperativesWithoutFailing() {
        IncidentContextDTO incident = incident("MEDIUM", "PLASTICO", "LOW", null, null);
        CooperativeDataDTO unnamed = cooperative(null, ORIGIN_LATITUDE, "5", "PLASTICO");
        CooperativeDataDTO named = cooperative("Named", ORIGIN_LATITUDE, "5", "PLASTICO");

        assertThat(calculator.score(incident, List.of(named, unnamed), 100)).hasSize(2);
    }

    private double distanceTo(CooperativeDataDTO cooperative) {
        return GeoDistance.kilometers(
                ORIGIN_LATITUDE, ORIGIN_LONGITUDE, cooperative.latitude(), cooperative.longitude());
    }

    private IncidentContextDTO incident(
            String priority,
            String category,
            String defaultRisk,
            String contamination,
            AiReportDTO aiReport
    ) {
        return new IncidentContextDTO(
                UUID.randomUUID(),
                priority,
                new WasteTypeDTO(category, defaultRisk),
                contamination,
                new CoordinatesDTO(ORIGIN_LATITUDE, ORIGIN_LONGITUDE),
                aiReport);
    }

    private CooperativeDataDTO cooperative(String name, BigDecimal latitude, String rating, String specialties) {
        BigDecimal averageRating = rating == null ? null : new BigDecimal(rating);
        List<String> specialtyList = specialties == null ? null : List.of(specialties.split(","));
        return new CooperativeDataDTO(UUID.randomUUID(), name, latitude, ORIGIN_LONGITUDE, averageRating, specialtyList);
    }
}
