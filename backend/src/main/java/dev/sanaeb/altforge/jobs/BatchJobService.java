package dev.sanaeb.altforge.jobs;

import dev.sanaeb.altforge.gemini.GeminiProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Persists a new {@link BatchJob} together with its per-image rows and hands
 * the work over to {@link BatchJobAsyncWorker}. Synchronous part of the
 * submit flow — the HTTP thread blocks here only long enough to write the
 * initial rows.
 */
@Service
public class BatchJobService {

    private final BatchJobRepository jobs;
    private final BatchJobItemRepository items;
    private final BatchJobAsyncWorker worker;
    private final GeminiProperties geminiProperties;

    public BatchJobService(
            BatchJobRepository jobs,
            BatchJobItemRepository items,
            BatchJobAsyncWorker worker,
            GeminiProperties geminiProperties) {
        this.jobs = jobs;
        this.items = items;
        this.worker = worker;
        this.geminiProperties = geminiProperties;
    }

    /**
     * Atomically persist the parent {@link BatchJob} + one {@link BatchJobItem} per
     * image, then trigger the async worker. The returned {@code job.id} is what
     * the client uses to poll {@code GET /api/jobs/{id}}.
     */
    @Transactional
    public BatchJob submit(String clientIpHash, String language, List<ImagePayload> images) {
        BatchJob job = new BatchJob();
        job.setId(UUID.randomUUID());
        job.setClientIpHash(clientIpHash);
        job.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        job.setStatus(BatchJobStatus.PENDING);
        job.setLanguage(language);
        job.setTotalImages((short) images.size());
        job.setProcessedImages((short) 0);
        job.setModel(geminiProperties.model());
        jobs.save(job);

        for (ImagePayload p : images) {
            BatchJobItem item = new BatchJobItem();
            item.setJobId(job.getId());
            item.setPosition(p.position());
            item.setFileName(p.fileName());
            item.setSizeBytes(p.sizeBytes());
            if (!p.isValid()) {
                item.setErrorCode(p.validationError());
                item.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
            }
            items.save(item);
        }

        worker.process(job.getId(), language, images);
        return job;
    }
}
