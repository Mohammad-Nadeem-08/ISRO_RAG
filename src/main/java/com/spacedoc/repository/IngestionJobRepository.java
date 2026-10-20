package com.spacedoc.repository;

import com.spacedoc.entity.IngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {
    Optional<IngestionJob> findByJobKey(String jobKey);
}
