package dev.sanaeb.altforge.web;

import dev.sanaeb.altforge.audit.RequestAuditRepository;
import dev.sanaeb.altforge.audit.RequestAuditService;
import dev.sanaeb.altforge.gemini.GeminiProperties;
import dev.sanaeb.altforge.jobs.BatchJob;
import dev.sanaeb.altforge.jobs.BatchJobItem;
import dev.sanaeb.altforge.jobs.BatchJobItemRepository;
import dev.sanaeb.altforge.jobs.BatchJobRepository;
import dev.sanaeb.altforge.jobs.BatchJobService;
import dev.sanaeb.altforge.jobs.BatchJobStatus;
import dev.sanaeb.altforge.ratelimit.RateLimitProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchJobController.class)
class BatchJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchJobService service;

    @MockitoBean
    private BatchJobRepository jobs;

    @MockitoBean
    private BatchJobItemRepository items;

    @MockitoBean
    private RequestAuditService requestAuditService;

    @MockitoBean
    private RequestAuditRepository requestAuditRepository;

    @MockitoBean
    private RateLimitProperties rateLimitProperties;

    @MockitoBean
    private GeminiProperties geminiProperties;

    @Test
    @DisplayName("Submit should return 202 + job id and forward to the service")
    void submitReturnsAccepted() throws Exception {
        UUID id = UUID.randomUUID();
        BatchJob job = newJob(id, BatchJobStatus.PENDING, 2);
        given(service.submit(anyString(), eq("fr"), any())).willReturn(job);

        MockMultipartFile a = new MockMultipartFile("images", "a.jpg", "image/jpeg", "a".getBytes());
        MockMultipartFile b = new MockMultipartFile("images", "b.jpg", "image/jpeg", "b".getBytes());

        mockMvc.perform(multipart("/api/alt-text/batch/async").file(a).file(b).param("lang", "fr"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalImages").value(2))
                .andExpect(jsonPath("$.acceptedImages").value(2))
                .andExpect(jsonPath("$.rejectedImages").value(0));
    }

    @Test
    @DisplayName("Submit should count invalid uploads in rejectedImages without failing the request")
    void submitCountsRejected() throws Exception {
        UUID id = UUID.randomUUID();
        BatchJob job = newJob(id, BatchJobStatus.PENDING, 2);
        given(service.submit(anyString(), anyString(), any())).willReturn(job);

        MockMultipartFile ok = new MockMultipartFile("images", "ok.jpg", "image/jpeg", "ok".getBytes());
        MockMultipartFile pdf = new MockMultipartFile("images", "doc.pdf", "application/pdf", "pdf".getBytes());

        mockMvc.perform(multipart("/api/alt-text/batch/async").file(ok).file(pdf))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.acceptedImages").value(1))
                .andExpect(jsonPath("$.rejectedImages").value(1));
    }

    @Test
    @DisplayName("Submit should reject batches over the 10-image cap with 400")
    void submitRejectsOverLimit() throws Exception {
        var request = multipart("/api/alt-text/batch/async");
        for (int i = 0; i < 11; i++) {
            request = request.file(new MockMultipartFile(
                    "images", "img-" + i + ".jpg", "image/jpeg", "x".getBytes()));
        }
        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Status endpoint should return current job snapshot with items")
    void statusReturnsSnapshot() throws Exception {
        UUID id = UUID.randomUUID();
        BatchJob job = newJob(id, BatchJobStatus.RUNNING, 2);
        job.setProcessedImages((short) 1);
        given(jobs.findById(id)).willReturn(Optional.of(job));

        BatchJobItem done = new BatchJobItem();
        done.setJobId(id);
        done.setPosition((short) 0);
        done.setFileName("a.jpg");
        done.setSizeBytes(10);
        done.setAltText("A red apple.");
        done.setLanguage("en");
        done.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));

        BatchJobItem pending = new BatchJobItem();
        pending.setJobId(id);
        pending.setPosition((short) 1);
        pending.setFileName("b.jpg");
        pending.setSizeBytes(20);

        given(items.findByJobIdOrderByPositionAsc(id)).willReturn(List.of(done, pending));

        mockMvc.perform(get("/api/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.totalImages").value(2))
                .andExpect(jsonPath("$.processedImages").value(1))
                .andExpect(jsonPath("$.items[0].altText").value("A red apple."))
                .andExpect(jsonPath("$.items[1].altText").doesNotExist());
    }

    @Test
    @DisplayName("Status endpoint should return 404 for an unknown job id")
    void statusReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        given(jobs.findById(id)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/{id}", id))
                .andExpect(status().isNotFound());
    }

    private static BatchJob newJob(UUID id, BatchJobStatus status, int total) {
        BatchJob job = new BatchJob();
        job.setId(id);
        job.setStatus(status);
        job.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        job.setLanguage("en");
        job.setTotalImages((short) total);
        job.setProcessedImages((short) 0);
        job.setModel("gemini-2.0-flash");
        return job;
    }
}
