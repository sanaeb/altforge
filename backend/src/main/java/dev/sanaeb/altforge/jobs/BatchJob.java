package dev.sanaeb.altforge.jobs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_jobs")
public class BatchJob {

    @Id
    private UUID id;

    @Column(name = "client_ip_hash", nullable = false, length = 64)
    private String clientIpHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BatchJobStatus status;

    @Column(nullable = false, length = 8)
    private String language;

    @Column(name = "total_images", nullable = false)
    private short totalImages;

    @Column(name = "processed_images", nullable = false)
    private short processedImages;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    public BatchJob() {
    }

    public UUID getId() { return id; }
    public String getClientIpHash() { return clientIpHash; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public BatchJobStatus getStatus() { return status; }
    public String getLanguage() { return language; }
    public short getTotalImages() { return totalImages; }
    public short getProcessedImages() { return processedImages; }
    public String getModel() { return model; }
    public String getErrorCode() { return errorCode; }

    public void setId(UUID id) { this.id = id; }
    public void setClientIpHash(String clientIpHash) { this.clientIpHash = clientIpHash; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public void setStatus(BatchJobStatus status) { this.status = status; }
    public void setLanguage(String language) { this.language = language; }
    public void setTotalImages(short totalImages) { this.totalImages = totalImages; }
    public void setProcessedImages(short processedImages) { this.processedImages = processedImages; }
    public void setModel(String model) { this.model = model; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
