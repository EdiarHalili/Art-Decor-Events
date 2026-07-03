package com.artdecor.workforce.api;

import com.artdecor.workforce.application.settings.AppSettingsResponse;
import com.artdecor.workforce.application.settings.AppSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class PublicSettingsController {
    private final AppSettingsService settings;

    public PublicSettingsController(AppSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public AppSettingsResponse current() {
        return settings.current();
    }
}
