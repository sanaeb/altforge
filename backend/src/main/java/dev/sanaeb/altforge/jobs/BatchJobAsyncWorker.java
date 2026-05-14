package dev.sanaeb.altforge.jobs;

import dev.sanaeb.altforge.gemini.GeminiException;
import dev.sanaeb.altforge.gemini.GeminiVisionService;
import dev.sanaeb.altforge.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Runs Gemini calls in the background for an async batch job. Lives in its own
 * bean so Spring's AOP proxy can honour {@code @Async} (self-invocation from
 * {@link BatchJobService} would bypass the proxy).
 *
 * <p>Each image is processed sequentially; the per-item row and the parent
 * job's {@code processed_images} counter are updated in a small transaction
 * after each call so the client polling {@code /api/jobs/{id}} sees results
 * stream in.
 */
@Service
public class BatchJobAsyncWorker {

    private static final Logger log = LoggerFactory.getLogger(BatchJobAsyncWorker.class);

    private final BatchJobRepository jobs;
    private final BatchJobItemRepository items;
    private final GeminiVisionService gemini;

    public BatchJobAsyncWorker(
            BatchJobRepository jobs,
            BatchJobItemRepository items,
            GeminiVisionService gemini) {
        this.jobs = jobs;
        this.items = items;
        this.gemini = gemini;
    }

    @Async("altforgeJobExecutor")
    public void process(UUID jobId, String language, List<ImagePayload> payloads) {
        markStarted(jobId);
        Language lang = Language.fromString(language);
        List<BatchJobItem> existing = items.findByJobIdOrderByPositionAsc(jobId);

        for (int i = 0; i < payloads.size(); i++) {
            ImagePayload p = payloads.get(i);
            BatchJobItem row = existing.get(i);
            try {
                if (!p.isValid()) {
                    incrementProcessed(jobId);
                    continue;
                }
                String altText = gemini.generateAltText(p.bytes(), p.contentType(), lang);
                completeSuccess(row, altText, lang.iso());
            } catch (GeminiException e) {
                log.warn("Async job {} item {} failed: {}", jobId, p.position(), e.getMessage());
                completeFailure(row, "gemini_unavailable");
            } catch (RuntimeException e) {
                log.warn("Async job {} item {} crashed: {}", jobId, p.position(), e.getMessage());
                completeFailure(row, "internal_error");
            }
            incrementProcessed(jobId);
        }

        markCompleted(jobId);
    }

    private void markStarted(UUID jobId) {
        jobs.findById(jobId).ifPresent(job -> {
            job.setStatus(BatchJobStatus.RUNNING);
            job.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
            jobs.save(job);
        });
    }

    private void completeSuccess(BatchJobItem row, String altText, String iso) {
        row.setAltText(altText);
        row.setLanguage(iso);
        row.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        items.save(row);
    }

    private void completeFailure(BatchJobItem row, String errorCode) {
        row.setErrorCode(errorCode);
        row.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        items.save(row);
    }

    private void incrementProcessed(UUID jobId) {
        jobs.findById(jobId).ifPresent(job -> {
            job.setProcessedImages((short) (job.getProcessedImages() + 1));
            jobs.save(job);
        });
    }

    private void markCompleted(UUID jobId) {
        jobs.findById(jobId).ifPresent(job -> {
            job.setStatus(BatchJobStatus.SUCCEEDED);
            job.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
            jobs.save(job);
        });
    }
}
