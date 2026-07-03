package com.artdecor.workforce.application.settings;

import com.artdecor.workforce.infrastructure.persistence.AppSettingsEntity;
import com.artdecor.workforce.infrastructure.persistence.AppSettingsRepository;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppSettingsService {
    private final AppSettingsRepository settings;

    public AppSettingsService(AppSettingsRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public AppSettingsResponse current() {
        return toResponse(currentEntity());
    }

    @Transactional
    public AppSettingsResponse update(AppSettingsCommand command) {
        validate(command);
        AppSettingsEntity entity = currentEntity();
        entity.setCompanyName(command.companyName().trim());
        entity.setLogoUrl(blankToNull(command.logoUrl()));
        entity.setPrimaryColor(command.primaryColor());
        entity.setAccentColor(command.accentColor());
        entity.setTimezone(command.timezone());
        entity.setDefaultCheckInOpenTime(command.defaultCheckInOpenTime());
        entity.setDefaultCheckInCloseTime(command.defaultCheckInCloseTime());
        entity.setAllowedLateMinutes(command.allowedLateMinutes());
        entity.setGpsEnabled(command.gpsEnabled());
        entity.setWorkplaceLatitude(command.workplaceLatitude());
        entity.setWorkplaceLongitude(command.workplaceLongitude());
        entity.setNotificationsEnabled(command.notificationsEnabled());
        entity.setSessionTimeoutMinutes(command.sessionTimeoutMinutes());
        return toResponse(entity);
    }

    public AppSettingsEntity currentEntity() {
        return settings.findAll().stream().findFirst().orElseGet(() -> settings.save(new AppSettingsEntity()));
    }

    private void validate(AppSettingsCommand command) {
        if (command.companyName() == null || command.companyName().isBlank()) {
            throw new IllegalArgumentException("Company name is required.");
        }
        ZoneId.of(command.timezone());
        if (!isHexColor(command.primaryColor()) || !isHexColor(command.accentColor())) {
            throw new IllegalArgumentException("Brand colors must use hex format, for example #c9a052.");
        }
        if (!command.defaultCheckInCloseTime().isAfter(command.defaultCheckInOpenTime())) {
            throw new IllegalArgumentException("Default check-in close time must be after opening time.");
        }
        if (command.allowedLateMinutes() < 0 || command.allowedLateMinutes() > 240) {
            throw new IllegalArgumentException("Allowed late minutes must be between 0 and 240.");
        }
        if (command.sessionTimeoutMinutes() < 5 || command.sessionTimeoutMinutes() > 1440) {
            throw new IllegalArgumentException("Session timeout must be between 5 minutes and 24 hours.");
        }
        if ((command.workplaceLatitude() == null) != (command.workplaceLongitude() == null)) {
            throw new IllegalArgumentException("Workplace latitude and longitude must be configured together.");
        }
        if (command.workplaceLatitude() != null && (command.workplaceLatitude() < -90 || command.workplaceLatitude() > 90)) {
            throw new IllegalArgumentException("Workplace latitude must be between -90 and 90.");
        }
        if (command.workplaceLongitude() != null && (command.workplaceLongitude() < -180 || command.workplaceLongitude() > 180)) {
            throw new IllegalArgumentException("Workplace longitude must be between -180 and 180.");
        }
    }

    private boolean isHexColor(String value) {
        return value != null && value.matches("^#[0-9a-fA-F]{6}$");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AppSettingsResponse toResponse(AppSettingsEntity entity) {
        return new AppSettingsResponse(
                entity.getCompanyName(),
                entity.getLogoUrl(),
                entity.getPrimaryColor(),
                entity.getAccentColor(),
                entity.getTimezone(),
                entity.getDefaultCheckInOpenTime() == null ? LocalTime.of(6, 50) : entity.getDefaultCheckInOpenTime(),
                entity.getDefaultCheckInCloseTime() == null ? LocalTime.of(7, 10) : entity.getDefaultCheckInCloseTime(),
                entity.getAllowedLateMinutes(),
                entity.isGpsEnabled(),
                entity.getWorkplaceLatitude(),
                entity.getWorkplaceLongitude(),
                entity.isNotificationsEnabled(),
                entity.getSessionTimeoutMinutes(),
                entity.getUpdatedAt()
        );
    }
}
