package com.backend.gapfinder.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.backend.gapfinder.models.RefreshTokenModel;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenModel, Long> {
    Optional<RefreshTokenModel> findByToken(String token);
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
