package com.backend.gapfinder.repositories;

import com.backend.gapfinder.enums.FriendshipStatusEnum;
import com.backend.gapfinder.models.FriendshipModel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<FriendshipModel, Long> {

    // Finds the friendships with a given status where the user is either the requester or the receiver
    @Query("""
        SELECT f
        FROM FriendshipModel f
        WHERE f.status = :status
          AND (f.requester.id = :userId OR f.receiver.id = :userId)
        """)
    List<FriendshipModel> findByUserAndStatus(@Param("userId") Long userId,
                                              @Param("status") FriendshipStatusEnum status);
}