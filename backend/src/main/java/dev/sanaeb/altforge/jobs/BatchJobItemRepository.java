package dev.sanaeb.altforge.jobs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BatchJobItemRepository extends JpaRepository<BatchJobItem, Long> {
    List<BatchJobItem> findByJobIdOrderByPositionAsc(UUID jobId);
}
