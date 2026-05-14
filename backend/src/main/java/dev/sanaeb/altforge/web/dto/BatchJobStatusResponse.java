package dev.sanaeb.altforge.web.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Snapshot of an async batch job and its items, as returned by
 * {@code GET /api/jobs/{id}}.
 */
public record BatchJobStatusResponse(
        UUID id,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String model,
        String language,
        int totalImages,
        int processedImages,
        String errorCode,
        List<Item> items) {

    public record Item(
            int position,
            String fileName,
            long sizeBytes,
            String altText,
            String language,
            String errorCode,
            OffsetDateTime completedAt) {
    }
}
