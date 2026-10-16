package com.volta.ranking.utils;

import java.math.BigDecimal;

public final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoDistance() {
    }

    public static double kilometers(
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude
    ) {
        double latitude1 = Math.toRadians(fromLatitude.doubleValue());
        double latitude2 = Math.toRadians(toLatitude.doubleValue());
        double deltaLatitude = latitude2 - latitude1;
        double deltaLongitude = Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());

        double sinLatitude = Math.sin(deltaLatitude / 2);
        double sinLongitude = Math.sin(deltaLongitude / 2);
        double haversine = sinLatitude * sinLatitude
                + Math.cos(latitude1) * Math.cos(latitude2) * sinLongitude * sinLongitude;

        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(haversine)));
    }
}
