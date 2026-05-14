package dev.sanaeb.altforge.jobs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_job_items")
public class BatchJobItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private short position;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "alt_text", columnDefinition = "TEXT")
    private String altText;

    @Column(length = 8)
    private String language;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public BatchJobItem() {
    }

    public Long getId() { return id; }
    public UUID getJobId() { return jobId; }
    public short getPosition() { return position; }
    public String getFileName() { return fileName; }
    public long getSizeBytes() { return sizeBytes; }
    public String getAltText() { return altText; }
    public String getLanguage() { return language; }
    public String getErrorCode() { return errorCode; }
    public OffsetDateTime getCompletedAt() { return completedAt; }

    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public void setPosition(short position) { this.position = position; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public void setAltText(String altText) { this.altText = altText; }
    public void setLanguage(String language) { this.language = language; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}
