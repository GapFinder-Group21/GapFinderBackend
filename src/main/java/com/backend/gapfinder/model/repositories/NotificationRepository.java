package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.NotificationModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationModel, Long> {

    // All notifications of a user, newest first
    List<NotificationModel> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    // Only unread notifications of a user, newest first
    List<NotificationModel> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(Long recipientId);
}
