package com.artdecor.workforce.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitingFilterTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-03T06:55:00Z"));

    @Test
    void ignoresForwardedForFromUntrustedRemoteAddress() throws Exception {
        RateLimitingFilter filter = filter(new RateLimitProperties(2, 1, 100, 5, List.of("10.0.0.10")));

        MockHttpServletRequest first = request("203.0.113.20", "198.51.100.1");
        MockHttpServletRequest second = request("203.0.113.20", "198.51.100.2");

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, new MockFilterChain());
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void usesForwardedForOnlyFromTrustedProxy() throws Exception {
        RateLimitingFilter filter = filter(new RateLimitProperties(2, 1, 100, 5, List.of("10.0.0.10")));

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(request("10.0.0.10", "198.51.100.1"), firstResponse, new MockFilterChain());
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(request("10.0.0.10", "198.51.100.2"), secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void expiresOldBuckets() throws Exception {
        RateLimitingFilter filter = filter(new RateLimitProperties(2, 1, 100, 1, List.of()));

        MockHttpServletRequest request = request("203.0.113.20", null);
        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilter(request, limited, new MockFilterChain());
        assertThat(limited.getStatus()).isEqualTo(429);

        clock.setInstant(Instant.parse("2026-07-03T06:57:00Z"));
        MockHttpServletResponse afterExpiry = new MockHttpServletResponse();
        filter.doFilter(request, afterExpiry, new MockFilterChain());

        assertThat(afterExpiry.getStatus()).isEqualTo(200);
    }

    private RateLimitingFilter filter(RateLimitProperties properties) {
        return new RateLimitingFilter(new ObjectMapper(), clock, properties);
    }

    private MockHttpServletRequest request(String remoteAddress, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/admin/login");
        request.setRemoteAddr(remoteAddress);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
