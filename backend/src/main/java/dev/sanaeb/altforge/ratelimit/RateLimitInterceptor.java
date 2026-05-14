package dev.sanaeb.altforge.ratelimit;

import dev.sanaeb.altforge.audit.RequestAuditInterceptor;
import dev.sanaeb.altforge.audit.RequestAuditRepository;
import dev.sanaeb.altforge.audit.RequestAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Rejects requests with 429 when an IP has exceeded {@code maxRequests} calls
 * over the last {@code windowMinutes}. Backed by the same {@code request_audits}
 * table used by /api/stats — no extra storage. Hashing matches
 * {@link RequestAuditService} so counts line up with what gets audited.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final String JSON =
            "{\"error\":\"rate_limited\",\"message\":\"Too many requests. Please retry in %d seconds.\"}";

    private final RateLimitProperties properties;
    private final RequestAuditRepository repository;

    public RateLimitInterceptor(RateLimitProperties properties, RequestAuditRepository repository) {
        this.properties = properties;
        this.repository = repository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!properties.enabled()) {
            return true;
        }
        String ip = RequestAuditService.resolveClientIp(request);
        String hash = sha256Hex(ip);
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(properties.windowMinutes());

        long count;
        try {
            count = repository.countByClientIpHashAndCreatedAtAfter(hash, since);
        } catch (RuntimeException e) {
            log.warn("Rate-limit check failed, allowing request through: {}", e.getMessage());
            return true;
        }

        if (count < properties.maxRequests()) {
            return true;
        }

        long retryAfterSeconds = (long) properties.windowMinutes() * 60;
        request.setAttribute(RequestAuditInterceptor.ATTR_ERROR_CODE, "rate_limited");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(String.format(JSON, retryAfterSeconds));
        return false;
    }

    /** Same digest as {@link RequestAuditService} so the audit hash matches the rate-limit lookup. */
    private static String sha256Hex(String input) {
        if (input == null || input.isBlank()) {
            return "0".repeat(64);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }
}
