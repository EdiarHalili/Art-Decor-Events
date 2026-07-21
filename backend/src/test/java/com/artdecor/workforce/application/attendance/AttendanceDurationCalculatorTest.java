package com.artdecor.workforce.application.attendance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AttendanceDurationCalculatorTest {
    private static final Instant START = Instant.parse("2026-07-03T06:00:00Z");

    @Test
    void roundsTwoMinutesAndZeroSecondsToTwoMinutes() {
        assertThat(roundedAfterSeconds(120)).isEqualTo(2);
    }

    @Test
    void roundsTwentyNineSecondsDownToZeroMinutes() {
        assertThat(roundedAfterSeconds(29)).isZero();
    }

    @Test
    void roundsThirtySecondsUpToOneMinute() {
        assertThat(roundedAfterSeconds(30)).isEqualTo(1);
    }

    @Test
    void roundsThirtyOneSecondsUpToOneMinute() {
        assertThat(roundedAfterSeconds(31)).isEqualTo(1);
    }

    @Test
    void roundsOneMinuteAndTwentyNineSecondsDownToOneMinute() {
        assertThat(roundedAfterSeconds(89)).isEqualTo(1);
    }

    @Test
    void roundsOneMinuteAndThirtySecondsUpToTwoMinutes() {
        assertThat(roundedAfterSeconds(90)).isEqualTo(2);
    }

    @Test
    void roundsOneMinuteAndThirtyOneSecondsUpToTwoMinutes() {
        assertThat(roundedAfterSeconds(91)).isEqualTo(2);
    }

    @Test
    void roundsTwoMinutesAndFiftyNineSecondsUpToThreeMinutes() {
        assertThat(roundedAfterSeconds(179)).isEqualTo(3);
    }

    @Test
    void roundsThreeMinutesAndZeroSecondsToThreeMinutes() {
        assertThat(roundedAfterSeconds(180)).isEqualTo(3);
    }

    @Test
    void roundsDurationsUnderThirtySecondsToZeroMinutes() {
        assertThat(roundedAfterSeconds(0)).isZero();
        assertThat(roundedAfterSeconds(29)).isZero();
    }

    private int roundedAfterSeconds(long seconds) {
        return AttendanceDurationCalculator.roundedMinutesBetween(START, START.plusSeconds(seconds));
    }
}
