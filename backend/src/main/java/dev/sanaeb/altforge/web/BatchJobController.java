package dev.sanaeb.altforge.web;

import dev.sanaeb.altforge.audit.RequestAuditInterceptor;
import dev.sanaeb.altforge.audit.RequestAuditService;
import dev.sanaeb.altforge.jobs.BatchJob;
import dev.sanaeb.altforge.jobs.BatchJobItem;
import dev.sanaeb.altforge.jobs.BatchJobItemRepository;
import dev.sanaeb.altforge.jobs.BatchJobRepository;
import dev.sanaeb.altforge.jobs.BatchJobService;
import dev.sanaeb.altforge.jobs.ImagePayload;
import dev.sanaeb.altforge.lang.Language;
import dev.sanaeb.altforge.web.dto.BatchJobStatusResponse;
import dev.sanaeb.altforge.web.dto.BatchJobSubmitResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class BatchJobController {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final int MAX_BATCH_SIZE = 10;

    private final BatchJobService service;
    private final BatchJobRepository jobs;
    private final BatchJobItemRepository items;

    public BatchJobController(
            BatchJobService service,
            BatchJobRepository jobs,
            BatchJobItemRepository items) {
        this.service = service;
        this.jobs = jobs;
        this.items = items;
    }

    @PostMapping(path = "/api/alt-text/batch/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchJobSubmitResponse> submit(
            HttpServletRequest request,
            @RequestParam("images") List<MultipartFile> uploads,
            @RequestParam(value = "lang", defaultValue = "en") String lang) throws IOException {
        if (uploads == null || uploads.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one image is required.");
        }
        if (uploads.size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Batch size is limited to " + MAX_BATCH_SIZE + " images.");
        }

        Language language = Language.fromString(lang);
        List<ImagePayload> payloads = new ArrayList<>(uploads.size());
        long totalBytes = 0;
        int rejected = 0;

        for (short i = 0; i < uploads.size(); i++) {
            MultipartFile f = uploads.get(i);
            String error = validate(f);
            totalBytes += f.getSize();
            payloads.add(new ImagePayload(
                    i,
                    f.getOriginalFilename(),
                    f.getContentType(),
                    f.getSize(),
                    error == null ? f.getBytes() : new byte[0],
                    error));
            if (error != null) rejected++;
        }

        request.setAttribute(RequestAuditInterceptor.ATTR_IMAGES_COUNT, uploads.size());
        request.setAttribute(RequestAuditInterceptor.ATTR_TOTAL_BYTES, totalBytes);

        String clientIpHash = sha256Hex(RequestAuditService.resolveClientIp(request));
        BatchJob job = service.submit(clientIpHash, language.iso(), payloads);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new BatchJobSubmitResponse(
                job.getId(),
                job.getStatus().name(),
                uploads.size(),
                uploads.size() - rejected,
                rejected));
    }

    @GetMapping("/api/jobs/{id}")
    public ResponseEntity<BatchJobStatusResponse> status(@PathVariable UUID id) {
        BatchJob job = jobs.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown job id."));

        List<BatchJobStatusResponse.Item> mapped = items.findByJobIdOrderByPositionAsc(id).stream()
                .map(BatchJobController::toDto)
                .toList();

        return ResponseEntity.ok(new BatchJobStatusResponse(
                job.getId(),
                job.getStatus().name(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getModel(),
                job.getLanguage(),
                job.getTotalImages(),
                job.getProcessedImages(),
                job.getErrorCode(),
                mapped));
    }

    private static BatchJobStatusResponse.Item toDto(BatchJobItem row) {
        return new BatchJobStatusResponse.Item(
                row.getPosition(),
                row.getFileName(),
                row.getSizeBytes(),
                row.getAltText(),
                row.getLanguage(),
                row.getErrorCode(),
                row.getCompletedAt());
    }

    private static String validate(MultipartFile f) {
        if (f.isEmpty()) return "empty_file";
        String contentType = f.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) return "invalid_image";
        if (f.getSize() > MAX_BYTES) return "too_large";
        return null;
    }

    private static String sha256Hex(String input) {
        if (input == null || input.isBlank()) return "0".repeat(64);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : bytes) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }
}
