package com.artdecor.workforce.infrastructure.security;

import com.artdecor.workforce.api.ApiExceptionHandler.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final RateLimitProperties properties;

    public RateLimitingFilter(ObjectMapper objectMapper, Clock clock, RateLimitProperties properties) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = path.startsWith("/api/v1/auth/")
                ? properties.resolvedAuthLimitPerMinute()
                : properties.resolvedDefaultLimitPerMinute();
        String key = clientKey(request) + ":" + path;
        long minute = Instant.now(clock).getEpochSecond() / 60;
        pruneExpiredBuckets(minute);
        Bucket bucket = buckets.compute(key, (ignored, current) -> {
            if (current == null || current.minute != minute) {
                return new Bucket(minute, 1);
            }
            current.count += 1;
            return current;
        });

        if (bucket.count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), new ApiError(
                    "RATE_LIMITED",
                    "Ka shumë kërkesa. Ju lutemi prisni pak dhe provoni përsëri.",
                    Map.of("retryAfterSeconds", 60)
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && isTrustedProxy(request.getRemoteAddr())) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void pruneExpiredBuckets(long currentMinute) {
        long oldestAllowedMinute = currentMinute - properties.resolvedBucketTtlMinutes();
        if (buckets.size() <= properties.resolvedMaxBuckets()) {
            buckets.entrySet().removeIf(entry -> entry.getValue().minute < oldestAllowedMinute);
            return;
        }
        buckets.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute);
        if (buckets.size() > properties.resolvedMaxBuckets()) {
            buckets.clear();
        }
    }

    private boolean isTrustedProxy(String remoteAddress) {
        List<String> trustedProxies = properties.trustedProxies() == null ? List.of() : properties.trustedProxies();
        if (trustedProxies.isEmpty() || remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        for (String trustedProxy : trustedProxies) {
            String value = trustedProxy.trim();
            if (value.equals(remoteAddress) || cidrMatches(value, remoteAddress)) {
                return true;
            }
        }
        return false;
    }

    private boolean cidrMatches(String cidr, String remoteAddress) {
        if (!cidr.contains("/")) {
            return false;
        }
        String[] parts = cidr.split("/", 2);
        try {
            byte[] trustedBytes = InetAddress.getByName(parts[0]).getAddress();
            byte[] remoteBytes = InetAddress.getByName(remoteAddress).getAddress();
            if (trustedBytes.length != remoteBytes.length) {
                return false;
            }
            int prefix = Integer.parseInt(parts[1]);
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int index = 0; index < fullBytes; index++) {
                if (trustedBytes[index] != remoteBytes[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = (-1) << (8 - remainingBits);
            return (trustedBytes[fullBytes] & mask) == (remoteBytes[fullBytes] & mask);
        } catch (UnknownHostException | NumberFormatException | ArrayIndexOutOfBoundsException exception) {
            return false;
        }
    }

    private static final class Bucket {
        private final long minute;
        private int count;

        private Bucket(long minute, int count) {
            this.minute = minute;
            this.count = count;
        }
    }
}
