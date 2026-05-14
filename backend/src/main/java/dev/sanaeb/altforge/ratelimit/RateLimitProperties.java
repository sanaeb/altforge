package dev.sanaeb.altforge.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the per-IP rate limit applied to {@code /api/alt-text} and
 * {@code /api/alt-text/batch}. Disabled by default — set
 * {@code altforge.ratelimit.enabled=true} to turn it on.
 *
 * @param enabled        master switch
 * @param windowMinutes  sliding window over which calls are counted (1–1440)
 * @param maxRequests    upper bound of calls per IP within the window
 */
@ConfigurationProperties(prefix = "altforge.ratelimit")
public record RateLimitProperties(
        boolean enabled,
        int windowMinutes,
        int maxRequests) {

    public RateLimitProperties {
        if (windowMinutes <= 0) windowMinutes = 60;
        if (maxRequests <= 0) maxRequests = 60;
    }
}
