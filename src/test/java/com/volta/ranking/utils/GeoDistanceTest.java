package com.volta.ranking.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoDistanceTest {

    @Test
    void isZeroForTheSamePoint() {
        BigDecimal latitude = new BigDecimal("-23.550520");
        BigDecimal longitude = new BigDecimal("-46.633308");

        assertThat(GeoDistance.kilometers(latitude, longitude, latitude, longitude)).isZero();
    }

    @Test
    void measuresOneDegreeOfLatitudeAsAboutOneHundredElevenKilometers() {
        double distance = GeoDistance.kilometers(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO);

        assertThat(distance).isCloseTo(111.19, within(0.05));
    }

    @Test
    void measuresTheDistanceBetweenSaoPauloAndRioDeJaneiro() {
        double distance = GeoDistance.kilometers(
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"),
                new BigDecimal("-22.906847"), new BigDecimal("-43.172896"));

        assertThat(distance).isCloseTo(360.0, within(5.0));
    }

    @Test
    void isSymmetric() {
        BigDecimal latitude1 = new BigDecimal("10");
        BigDecimal longitude1 = new BigDecimal("20");
        BigDecimal latitude2 = new BigDecimal("-30");
        BigDecimal longitude2 = new BigDecimal("40");

        assertThat(GeoDistance.kilometers(latitude1, longitude1, latitude2, longitude2))
                .isEqualTo(GeoDistance.kilometers(latitude2, longitude2, latitude1, longitude1));
    }

    @Test
    void handlesAntipodalPointsWithoutFailing() {
        double distance = GeoDistance.kilometers(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("180"));

        assertThat(distance).isCloseTo(20015.0, within(5.0));
    }
}
