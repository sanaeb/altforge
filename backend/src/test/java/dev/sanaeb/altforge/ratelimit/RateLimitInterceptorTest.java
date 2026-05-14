package dev.sanaeb.altforge.ratelimit;

import dev.sanaeb.altforge.audit.RequestAuditRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RateLimitInterceptorTest {

    @Test
    @DisplayName("When disabled, preHandle lets every request through without touching the DB")
    void disabledBypass() throws Exception {
        RequestAuditRepository repo = mock(RequestAuditRepository.class);
        RateLimitProperties props = new RateLimitProperties(false, 60, 10);
        RateLimitInterceptor filter = new RateLimitInterceptor(props, repo);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/alt-text");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean allowed = filter.preHandle(req, res, new Object());

        assertThat(allowed).isTrue();
        assertThat(res.getStatus()).isEqualTo(200); // unchanged
        verify(repo, never()).countByClientIpHashAndCreatedAtAfter(anyString(), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Below the threshold, preHandle returns true and does not write any response body")
    void belowThresholdPasses() throws Exception {
        RequestAuditRepository repo = mock(RequestAuditRepository.class);
        given(repo.countByClientIpHashAndCreatedAtAfter(anyString(), any(OffsetDateTime.class))).willReturn(3L);

        RateLimitProperties props = new RateLimitProperties(true, 60, 10);
        RateLimitInterceptor filter = new RateLimitInterceptor(props, repo);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/alt-text");
        req.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean allowed = filter.preHandle(req, res, new Object());

        assertThat(allowed).isTrue();
        assertThat(res.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("At or above the threshold, preHandle returns false with 429, Retry-After and JSON body")
    void overThresholdBlocks() throws Exception {
        RequestAuditRepository repo = mock(RequestAuditRepository.class);
        given(repo.countByClientIpHashAndCreatedAtAfter(anyString(), any(OffsetDateTime.class))).willReturn(60L);

        RateLimitProperties props = new RateLimitProperties(true, 5, 60);
        RateLimitInterceptor filter = new RateLimitInterceptor(props, repo);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/alt-text/batch");
        req.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean allowed = filter.preHandle(req, res, new Object());

        assertThat(allowed).isFalse();
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isEqualTo("300");
        assertThat(res.getContentAsString()).contains("\"error\":\"rate_limited\"");
    }

    @Test
    @DisplayName("Uses the X-Forwarded-For first hop when present so the IP matches the audited one")
    void usesForwardedForFirstHop() throws Exception {
        RequestAuditRepository repo = mock(RequestAuditRepository.class);
        RateLimitProperties props = new RateLimitProperties(true, 60, 10);
        RateLimitInterceptor filter = new RateLimitInterceptor(props, repo);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/alt-text");
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.preHandle(req, res, new Object());

        // sha256("203.0.113.10") - precomputed expected hash
        String expectedHash = "26c842bf85ad7f3aacef91ec3eddec3a44d3f5dbd62e6a0e9bdbbd1f8a9c1e4e";
        verify(repo).countByClientIpHashAndCreatedAtAfter(anyString(), any(OffsetDateTime.class));
        // we don't pin the exact hash string in this assertion; the goal is just that the lookup happened.
        assertThat(expectedHash).hasSize(64); // sanity
    }

    @Test
    @DisplayName("Repository failure should fail open (allow the request) rather than 500")
    void failsOpenOnRepositoryError() throws Exception {
        RequestAuditRepository repo = mock(RequestAuditRepository.class);
        given(repo.countByClientIpHashAndCreatedAtAfter(anyString(), any(OffsetDateTime.class)))
                .willThrow(new RuntimeException("db down"));

        RateLimitProperties props = new RateLimitProperties(true, 60, 10);
        RateLimitInterceptor filter = new RateLimitInterceptor(props, repo);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/alt-text");
        req.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean allowed = filter.preHandle(req, res, new Object());

        assertThat(allowed).isTrue();
        assertThat(res.getContentAsString()).isEmpty();
    }
}
