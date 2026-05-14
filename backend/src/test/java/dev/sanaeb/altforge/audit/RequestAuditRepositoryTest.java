package dev.sanaeb.altforge.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RequestAuditRepositoryTest {

    @Autowired
    private RequestAuditRepository repository;

    private final OffsetDateTime t0 = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2);

    @BeforeEach
    void seed() {
        repository.deleteAll();
        save("single", "en", (short) 200, 120, t0);
        save("single", "en", (short) 200, 240, t0.plusMinutes(5));
        save("single", "fr", (short) 200, 180, t0.plusMinutes(10));
        save("batch", "fr", (short) 502, 300, t0.plusMinutes(15));
        save("single", "en", (short) 200, 90,  t0.plusMinutes(20));
    }

    @Test
    @DisplayName("Should count rows created within the window")
    void countsRowsInWindow() {
        long n = repository.countByCreatedAtAfter(t0.minusMinutes(1));
        assertThat(n).isEqualTo(5L);

        long recent = repository.countByCreatedAtAfter(t0.plusMinutes(12));
        assertThat(recent).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should count only 2xx/3xx rows when filtering by status_code < 400")
    void countsSuccessRowsOnly() {
        long ok = repository.countByCreatedAtAfterAndStatusCodeLessThan(t0.minusMinutes(1), (short) 400);
        assertThat(ok).isEqualTo(4L);
    }

    @Test
    @DisplayName("Should group request counts by language")
    void groupsByLanguage() {
        List<Object[]> rows = repository.countByLanguageSince(t0.minusMinutes(1));
        Map<String, Long> map = toMap(rows);
        assertThat(map).contains(entry("en", 3L), entry("fr", 2L));
    }

    @Test
    @DisplayName("Should group request counts by endpoint")
    void groupsByEndpoint() {
        List<Object[]> rows = repository.countByEndpointSince(t0.minusMinutes(1));
        Map<String, Long> map = toMap(rows);
        assertThat(map).contains(entry("single", 4L), entry("batch", 1L));
    }

    @Test
    @DisplayName("Should average latency only over successful (2xx/3xx) calls")
    void averagesLatencyOnSuccessOnly() {
        Double avg = repository.averageLatencyMsSince(t0.minusMinutes(1));
        assertThat(avg).isNotNull();
        // (120 + 240 + 180 + 90) / 4 = 157.5
        assertThat(avg).isEqualTo(157.5);
    }

    private void save(String endpoint, String lang, short statusCode, int latencyMs, OffsetDateTime when) {
        RequestAudit a = new RequestAudit();
        a.setCreatedAt(when);
        a.setEndpoint(endpoint);
        a.setClientIpHash("0".repeat(64));
        a.setLanguage(lang);
        a.setImagesCount((short) 1);
        a.setTotalBytes(1024);
        a.setStatusCode(statusCode);
        a.setLatencyMs(latencyMs);
        a.setModel("test-model");
        repository.save(a);
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(
                java.util.stream.Collectors.toMap(
                        r -> (String) r[0],
                        r -> ((Number) r[1]).longValue()));
    }
}
