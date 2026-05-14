package dev.sanaeb.altforge.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface RequestAuditRepository extends JpaRepository<RequestAudit, Long> {

    long countByCreatedAtAfter(OffsetDateTime since);

    long countByCreatedAtAfterAndStatusCodeLessThan(OffsetDateTime since, short statusCode);

    long countByClientIpHashAndCreatedAtAfter(String clientIpHash, OffsetDateTime since);

    /**
     * Number of calls per ISO language code over the window, ordered by count desc.
     * Returns rows as {@code [language, count]}.
     */
    @Query("""
            SELECT a.language, COUNT(a)
              FROM RequestAudit a
             WHERE a.createdAt >= :since
             GROUP BY a.language
             ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countByLanguageSince(@Param("since") OffsetDateTime since);

    /**
     * Number of calls per endpoint ("single" / "batch") over the window.
     * Returns rows as {@code [endpoint, count]}.
     */
    @Query("""
            SELECT a.endpoint, COUNT(a)
              FROM RequestAudit a
             WHERE a.createdAt >= :since
             GROUP BY a.endpoint
            """)
    List<Object[]> countByEndpointSince(@Param("since") OffsetDateTime since);

    /**
     * Average latency in milliseconds for successful (2xx) calls in the window,
     * or {@code null} when the window is empty.
     */
    @Query("""
            SELECT AVG(a.latencyMs)
              FROM RequestAudit a
             WHERE a.createdAt >= :since
               AND a.statusCode < 400
            """)
    Double averageLatencyMsSince(@Param("since") OffsetDateTime since);
}
