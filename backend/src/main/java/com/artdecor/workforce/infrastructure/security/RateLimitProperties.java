package com.artdecor.workforce.infrastructure.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.rate-limit")
public record RateLimitProperties(
        int defaultLimitPerMinute,
        int authLimitPerMinute,
        int maxBuckets,
        int bucketTtlMinutes,
        List<String> trustedProxies
) {
    public int resolvedDefaultLimitPerMinute() {
        return defaultLimitPerMinute <= 0 ? 180 : defaultLimitPerMinute;
    }

    public int resolvedAuthLimitPerMinute() {
        return authLimitPerMinute <= 0 ? 20 : authLimitPerMinute;
    }

    public int resolvedMaxBuckets() {
        return maxBuckets <= 0 ? 10_000 : maxBuckets;
    }

    public int resolvedBucketTtlMinutes() {
        return bucketTtlMinutes <= 0 ? 5 : bucketTtlMinutes;
    }
}
