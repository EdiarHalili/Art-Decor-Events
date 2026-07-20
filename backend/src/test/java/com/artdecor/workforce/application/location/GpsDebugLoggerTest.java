package com.artdecor.workforce.application.location;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GpsDebugLoggerTest {
    @AfterEach
    void tearDown() {
        System.clearProperty("artdecor.gps.debug");
    }

    @Test
    void disabledByDefault() throws Exception {
        System.clearProperty("artdecor.gps.debug");

        assertThat(enabled()).isFalse();
    }

    @Test
    void enabledOnlyWhenExplicitlyRequested() throws Exception {
        System.setProperty("artdecor.gps.debug", "true");

        assertThat(enabled()).isTrue();
    }

    private boolean enabled() throws Exception {
        Method method = GpsDebugLogger.class.getDeclaredMethod("enabled");
        method.setAccessible(true);
        return (boolean) method.invoke(null);
    }
}
