package dev.sanaeb.altforge.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * One row per call to /api/alt-text or /api/alt-text/batch. Client IPs are
 * hashed (SHA-256, hex-encoded) before storage so audits never contain raw
 * personal data.
 */
@Entity
@Table(name = "request_audits")
public class RequestAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** "single" for /api/alt-text, "batch" for /api/alt-text/batch. */
    @Column(nullable = false, length = 64)
    private String endpoint;

    /** SHA-256 hex digest of the remote IP, 64 characters. */
    @Column(name = "client_ip_hash", nullable = false, length = 64)
    private String clientIpHash;

    @Column(nullable = false, length = 8)
    private String language;

    @Column(name = "images_count", nullable = false)
    private short imagesCount;

    @Column(name = "total_bytes", nullable = false)
    private long totalBytes;

    @Column(name = "status_code", nullable = false)
    private short statusCode;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(length = 64)
    private String model;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    public RequestAudit() {
    }

    public Long getId() { return id; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getEndpoint() { return endpoint; }
    public String getClientIpHash() { return clientIpHash; }
    public String getLanguage() { return language; }
    public short getImagesCount() { return imagesCount; }
    public long getTotalBytes() { return totalBytes; }
    public short getStatusCode() { return statusCode; }
    public int getLatencyMs() { return latencyMs; }
    public String getModel() { return model; }
    public String getErrorCode() { return errorCode; }

    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setClientIpHash(String clientIpHash) { this.clientIpHash = clientIpHash; }
    public void setLanguage(String language) { this.language = language; }
    public void setImagesCount(short imagesCount) { this.imagesCount = imagesCount; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
    public void setStatusCode(short statusCode) { this.statusCode = statusCode; }
    public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    public void setModel(String model) { this.model = model; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
