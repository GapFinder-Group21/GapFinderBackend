package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.enums.FriendshipStatusEnum;
import com.backend.gapfinder.model.models.FriendshipModel;

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

    // Finds the friendships between two users, in either direction
    @Query("""
        SELECT f
        FROM FriendshipModel f
        WHERE (f.requester.id = :userA AND f.receiver.id = :userB)
           OR (f.requester.id = :userB AND f.receiver.id = :userA)
        """)
    List<FriendshipModel> findBetweenUsers(@Param("userA") Long userA,
                                           @Param("userB") Long userB);
}