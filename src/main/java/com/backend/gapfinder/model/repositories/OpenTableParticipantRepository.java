package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.OpenTableModel;
import com.backend.gapfinder.model.models.OpenTableParticipantModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpenTableParticipantRepository extends JpaRepository<OpenTableParticipantModel, Long> {

    // Checks if a student is already registered as a participant in a specific open table
    boolean existsByOpenTableIdAndUserId(Long openTableId, Long userId);

    // Count the open tables created at or after the given date
    long countByJoinedAtGreaterThanEqual(LocalDateTime since);

    // Counts the total number of participants for a given open table ID
    long countByOpenTableId(Long openTableId);

    // Retrieves all participant records associated with a specific open table ID
    List<OpenTableParticipantModel> findByOpenTableId(Long openTableId);

    // Finds a specific participant record by open table ID and user ID, wrapped in an Optional
    Optional<OpenTableParticipantModel> findByOpenTableIdAndUserId(Long openTableId, Long userId);

    // The open tables a user joined (not the ones they created), the most recent join first
    @Query("SELECT p.openTable FROM OpenTableParticipantModel p "
            + "WHERE p.user.id = :userId AND p.openTable.creator.id <> :userId "
            + "ORDER BY p.joinedAt DESC")
    List<OpenTableModel> findJoinedOpenTables(@Param("userId") Long userId);
}