package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.ChatMessageModel;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessageModel, Long> {

    // Messages between two users (in both directions) newer than afterId, oldest first.
    // The page limits how many come back in one call
    @Query("""
        SELECT m
        FROM ChatMessageModel m
        WHERE m.id > :afterId
          AND ((m.sender.id = :userA AND m.receiver.id = :userB)
            OR (m.sender.id = :userB AND m.receiver.id = :userA))
        ORDER BY m.id ASC
        """)
    List<ChatMessageModel> findConversationAfter(@Param("userA") Long userA,
                                                 @Param("userB") Long userB,
                                                 @Param("afterId") Long afterId,
                                                 Pageable page);

    // Whether this sender already sent a message with this client id
    boolean existsBySenderIdAndClientId(Long senderId, String clientId);
}
