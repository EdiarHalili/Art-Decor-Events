package com.artdecor.workforce.application.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.infrastructure.persistence.AppSettingsEntity;
import com.artdecor.workforce.infrastructure.persistence.AppSettingsRepository;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppSettingsServiceTest {
    private final AppSettingsRepository settings = org.mockito.Mockito.mock(AppSettingsRepository.class);
    private final AppSettingsService service = new AppSettingsService(settings);

    @Test
    void allowsDefaultCheckInCloseTimeAtMidnightForOvernightSchedules() {
        AppSettingsEntity entity = new AppSettingsEntity();
        when(settings.findAll()).thenReturn(List.of(entity));

        AppSettingsResponse response = service.update(new AppSettingsCommand(
                "Art Decor Events",
                null,
                "#c9a052",
                "#496f5d",
                "Europe/Berlin",
                LocalTime.of(18, 0),
                LocalTime.MIDNIGHT,
                0,
                true,
                null,
                null,
                false,
                false,
                10,
                60,
                false
        ));

        assertThat(response.defaultCheckInOpenTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(response.defaultCheckInCloseTime()).isEqualTo(LocalTime.MIDNIGHT);
    }
}
