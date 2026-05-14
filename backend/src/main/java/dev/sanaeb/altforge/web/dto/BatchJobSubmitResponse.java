package dev.sanaeb.altforge.web.dto;

import java.util.UUID;

/**
 * Returned by {@code POST /api/alt-text/batch/async}. The job has been
 * persisted and queued; clients poll {@code GET /api/jobs/{id}} to follow
 * its progress.
 */
public record BatchJobSubmitResponse(
        UUID id,
        String status,
        int totalImages,
        int acceptedImages,
        int rejectedImages) {
}
