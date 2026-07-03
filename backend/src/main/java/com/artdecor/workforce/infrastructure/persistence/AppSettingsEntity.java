package com.artdecor.workforce.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "app_settings")
public class AppSettingsEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String companyName = "Art Decor Events";

    private String logoUrl;

    @Column(nullable = false)
    private String primaryColor = "#c9a052";

    @Column(nullable = false)
    private String accentColor = "#496f5d";

    @Column(nullable = false)
    private String timezone = "Europe/Berlin";

    @Column(nullable = false)
    private LocalTime defaultCheckInOpenTime = LocalTime.of(6, 50);

    @Column(nullable = false)
    private LocalTime defaultCheckInCloseTime = LocalTime.of(7, 10);

    @Column(nullable = false)
    private int allowedLateMinutes;

    @Column(nullable = false)
    private boolean gpsEnabled = true;

    @Column(nullable = false)
    private boolean notificationsEnabled = true;

    @Column(nullable = false)
    private int sessionTimeoutMinutes = 60;

    private Instant updatedAt;

    public UUID getId() { return id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public LocalTime getDefaultCheckInOpenTime() { return defaultCheckInOpenTime; }
    public void setDefaultCheckInOpenTime(LocalTime defaultCheckInOpenTime) { this.defaultCheckInOpenTime = defaultCheckInOpenTime; }
    public LocalTime getDefaultCheckInCloseTime() { return defaultCheckInCloseTime; }
    public void setDefaultCheckInCloseTime(LocalTime defaultCheckInCloseTime) { this.defaultCheckInCloseTime = defaultCheckInCloseTime; }
    public int getAllowedLateMinutes() { return allowedLateMinutes; }
    public void setAllowedLateMinutes(int allowedLateMinutes) { this.allowedLateMinutes = allowedLateMinutes; }
    public boolean isGpsEnabled() { return gpsEnabled; }
    public void setGpsEnabled(boolean gpsEnabled) { this.gpsEnabled = gpsEnabled; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
