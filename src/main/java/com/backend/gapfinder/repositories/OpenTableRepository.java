package com.backend.gapfinder.repositories;

import com.backend.gapfinder.enums.OpenTableStatusEnum;
import com.backend.gapfinder.models.OpenTableModel;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpenTableRepository extends JpaRepository<OpenTableModel, Long> {

    // Finds OPEN tables in a building that overlap with a gap of the user that has not ended yet
    // SMART FEATURE
    @Query("""
        SELECT DISTINCT o
        FROM OpenTableModel o, GapModel g
        WHERE o.building.id = :buildingId
          AND o.status = :status
          AND o.endTime > :now
          AND g.user.id = :userId
          AND g.endTime > :now
          AND o.startTime < g.endTime
          AND o.endTime > g.startTime
        ORDER BY o.startTime
        """)
    List<OpenTableModel> findMatchingOpenTables(@Param("userId") Long userId,
                                                @Param("buildingId") Long buildingId,
                                                @Param("status") OpenTableStatusEnum status,
                                                @Param("now") LocalDateTime now);



    // Find the open tables with any of the given statuses whose end time is before the given time
    List<OpenTableModel> findByStatusInAndEndTimeBefore(List<OpenTableStatusEnum> statuses, LocalDateTime time);
}
