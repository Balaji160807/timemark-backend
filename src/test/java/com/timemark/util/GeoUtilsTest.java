package com.timemark.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoUtilsTest {

    @Test
    void distanceBetweenIdenticalPointsIsZero() {
        double d = GeoUtils.distanceMeters(6.9271, 79.8612, 6.9271, 79.8612);
        assertThat(d).isEqualTo(0.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void distanceBetweenColomboAndKandyIsRoughlyRight() {
        // Colombo Fort ~ (6.9344, 79.8428), Kandy ~ (7.2906, 80.6337)
        // Real-world distance is ~95km. Haversine should land within a few km of that.
        double d = GeoUtils.distanceMeters(6.9344, 79.8428, 7.2906, 80.6337);
        double km = d / 1000;
        assertThat(km).isBetween(85.0, 105.0);
    }

    @Test
    void shortDistanceWithinOfficeRadiusIsSmall() {
        // Two points ~50m apart (roughly 0.00045 degrees of latitude)
        double d = GeoUtils.distanceMeters(6.9271, 79.8612, 6.92755, 79.8612);
        assertThat(d).isLessThan(100);
    }
}
