package dev.sanaeb.altforge.web;

import dev.sanaeb.altforge.audit.RequestAuditRepository;
import dev.sanaeb.altforge.web.dto.StatsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only stats over the {@code request_audits} table. The window is
 * configurable via the {@code hours} query parameter and defaults to 24.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private static final int DEFAULT_WINDOW_HOURS = 24;
    private static final int MAX_WINDOW_HOURS = 24 * 30;

    private final RequestAuditRepository repository;

    public StatsController(RequestAuditRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<StatsResponse> stats(
            @RequestParam(value = "hours", defaultValue = "24") int hours) {
        int window = Math.min(Math.max(hours, 1), MAX_WINDOW_HOURS);
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusHours(window);

        long total = repository.countByCreatedAtAfter(since);
        long succeeded = repository.countByCreatedAtAfterAndStatusCodeLessThan(since, (short) 400);
        long failed = Math.max(total - succeeded, 0);
        double successRate = total == 0 ? 0.0 : Math.round((1000.0 * succeeded) / total) / 10.0;
        Double avgLatency = repository.averageLatencyMsSince(since);

        return ResponseEntity.ok(new StatsResponse(
                total,
                succeeded,
                failed,
                successRate,
                avgLatency,
                toCountMap(repository.countByLanguageSince(since)),
                toCountMap(repository.countByEndpointSince(since)),
                window));
    }

    private static Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] == null ? "" : row[0].toString();
            long count = row[1] instanceof Number n ? n.longValue() : 0L;
            out.put(key, count);
        }
        return out;
    }
}
