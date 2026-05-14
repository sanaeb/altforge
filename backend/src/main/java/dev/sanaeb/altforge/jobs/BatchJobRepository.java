package dev.sanaeb.altforge.jobs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BatchJobRepository extends JpaRepository<BatchJob, UUID> {
}
