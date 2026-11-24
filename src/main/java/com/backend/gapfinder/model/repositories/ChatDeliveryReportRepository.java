package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.ChatDeliveryReportModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repository of the chat delivery reports sent by the app (BQ 17 INDIVIDUAL TYPE 1)
@Repository
public interface ChatDeliveryReportRepository extends JpaRepository<ChatDeliveryReportModel, Long> {

    // Whether this user already reported this message
    boolean existsByUserIdAndClientId(Long userId, String clientId);
}
