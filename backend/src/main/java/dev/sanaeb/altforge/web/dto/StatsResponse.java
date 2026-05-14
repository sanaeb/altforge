package dev.sanaeb.altforge.web.dto;

import java.util.Map;

/**
 * Aggregated audit metrics computed from {@code request_audits} and returned
 * by {@code GET /api/stats}.
 *
 * @param totalRequests   number of calls received during the window
 * @param succeededRequests number of calls that returned a 2xx/3xx status
 * @param failedRequests  number of calls that returned a 4xx/5xx status
 * @param successRatePct  succeededRequests / totalRequests, rounded to 1 decimal
 * @param avgLatencyMs    average latency in milliseconds across successful calls
 * @param byLanguage      count of calls per ISO language code
 * @param byEndpoint      count of calls per endpoint ("single" / "batch")
 * @param windowHours     duration of the window in hours, echoed back for clarity
 */
public record StatsResponse(
        long totalRequests,
        long succeededRequests,
        long failedRequests,
        double successRatePct,
        Double avgLatencyMs,
        Map<String, Long> byLanguage,
        Map<String, Long> byEndpoint,
        int windowHours) {
}
