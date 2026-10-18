package com.backend.gapfinder.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.gapfinder.model.models.GoogleCredentialModel;

import java.util.Optional;

public interface GoogleCredentialRepository extends JpaRepository<GoogleCredentialModel, Long> {
    Optional<GoogleCredentialModel> findByUserId(Long userId);
}
