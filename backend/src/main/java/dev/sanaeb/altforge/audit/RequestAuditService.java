package dev.sanaeb.altforge.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Persists {@link RequestAudit} rows for every API call. Client IPs are
 * hashed (SHA-256, hex-encoded) before storage so raw IPs never reach the
 * database. The hash is stable across requests, which is what enables
 * per-IP rate limiting on top of the same table.
 */
@Service
public class RequestAuditService {

    private static final Logger log = LoggerFactory.getLogger(RequestAuditService.class);
    private static final String UNKNOWN_IP_HASH = "0".repeat(64);

    private final RequestAuditRepository repository;

    public RequestAuditService(RequestAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(
            String endpoint,
            String clientIp,
            String language,
            int imagesCount,
            long totalBytes,
            int statusCode,
            int latencyMs,
            String model,
            String errorCode) {
        try {
            RequestAudit audit = new RequestAudit();
            audit.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            audit.setEndpoint(endpoint);
            audit.setClientIpHash(hashIp(clientIp));
            audit.setLanguage(language == null ? "" : language);
            audit.setImagesCount((short) Math.min(imagesCount, Short.MAX_VALUE));
            audit.setTotalBytes(totalBytes);
            audit.setStatusCode((short) statusCode);
            audit.setLatencyMs(latencyMs);
            audit.setModel(model);
            audit.setErrorCode(errorCode);
            repository.save(audit);
        } catch (RuntimeException e) {
            log.warn("Failed to persist request audit for {}: {}", endpoint, e.getMessage());
        }
    }

    /** Best-effort client IP extraction: trust the first hop of X-Forwarded-For if present, else fall back to remote_addr. */
    public static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "" : remote;
    }

    private static String hashIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return UNKNOWN_IP_HASH;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
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
