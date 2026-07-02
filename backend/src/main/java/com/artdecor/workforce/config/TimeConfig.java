package com.artdecor.workforce.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
    @Bean
    public Clock clock(@Value("${app.business-zone:Europe/Berlin}") String businessZone) {
        return Clock.system(ZoneId.of(businessZone));
    }
}
