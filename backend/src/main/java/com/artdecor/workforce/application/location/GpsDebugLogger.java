package com.artdecor.workforce.application.location;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GpsDebugLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(GpsDebugLogger.class);

    private GpsDebugLogger() {
    }

    public static void log(String stage, Object... values) {
        if (!enabled()) {
            return;
        }
        LOGGER.info("GPS DEBUG [{}] {}", stage, Arrays.toString(values));
    }

    private static boolean enabled() {
        String explicit = System.getProperty("artdecor.gps.debug", System.getenv("ARTDECOR_GPS_DEBUG"));
        if (explicit != null) {
            return Boolean.parseBoolean(explicit);
        }
        String activeProfiles = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
        return activeProfiles == null || !activeProfiles.toLowerCase().contains("prod");
    }
}
