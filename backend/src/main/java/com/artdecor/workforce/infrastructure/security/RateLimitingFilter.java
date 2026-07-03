package com.artdecor.workforce.infrastructure.security;

import com.artdecor.workforce.api.ApiExceptionHandler.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final int DEFAULT_LIMIT_PER_MINUTE = 180;
    private static final int AUTH_LIMIT_PER_MINUTE = 20;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RateLimitingFilter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = path.startsWith("/api/v1/auth/") ? AUTH_LIMIT_PER_MINUTE : DEFAULT_LIMIT_PER_MINUTE;
        String key = clientKey(request) + ":" + path;
        long minute = Instant.now(clock).getEpochSecond() / 60;
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
                    "Too many requests. Please wait a moment and try again.",
                    Map.of("retryAfterSeconds", 60)
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
