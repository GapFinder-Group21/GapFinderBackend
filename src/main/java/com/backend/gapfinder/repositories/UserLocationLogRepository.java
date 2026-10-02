package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.UserLocationLogModel;
import com.backend.gapfinder.repositories.projections.FavoriteBuildingProjection;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserLocationLogRepository extends JpaRepository<UserLocationLogModel, Long> {

    // Calculates the building where the user has accumulated the most time during their gaps
    // SMART-FEATURE
    @Query(value = """
        SELECT b.id AS buildingId,
            b.name AS buildingName,
            SUM(t.minutes) AS totalMinutes
        FROM (
            SELECT l.building_id, l.user_id, l."timestamp" AS ts,
                LEAST(
                    EXTRACT(EPOCH FROM (
                        LEAD(l."timestamp") OVER (PARTITION BY l.user_id ORDER BY l."timestamp")
                        - l."timestamp")) / 60.0,
                    15) AS minutes
            FROM location_logs l
            WHERE l.user_id = :userId
        ) t
        JOIN buildings b ON b.id = t.building_id
        WHERE t.minutes IS NOT NULL
          AND EXISTS (
              SELECT 1 FROM gaps g
              WHERE g.user_id = t.user_id
                AND t.ts BETWEEN g.start_time AND g.end_time
          )
        GROUP BY b.id, b.name
        ORDER BY totalMinutes DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<FavoriteBuildingProjection> findFavoriteBuilding(@Param("userId") Long userId);
}