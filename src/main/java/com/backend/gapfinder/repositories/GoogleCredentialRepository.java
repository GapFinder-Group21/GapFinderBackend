package com.backend.gapfinder.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.gapfinder.models.GoogleCredentialModel;

import java.util.Optional;

public interface GoogleCredentialRepository extends JpaRepository<GoogleCredentialModel, Long> {
    Optional<GoogleCredentialModel> findByUserId(Long userId);
}
