package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.UserLocationLogModel;
import com.backend.gapfinder.model.repositories.projections.BuildingGapPresenceProjection;
import com.backend.gapfinder.model.repositories.projections.FavoriteBuildingProjection;
import com.backend.gapfinder.model.repositories.projections.LocationUpdateRangeProjection;
import com.backend.gapfinder.model.repositories.projections.LocationUpdateSummaryProjection;
import com.backend.gapfinder.model.repositories.projections.StudentLocationUpdateRateProjection;

import java.time.LocalDateTime;
import java.util.List;
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

        // Calculates student presence and free time metrics per building based on all the location logs during gaps
        // Buildings split in numbered parts (e.g. "Bobo 1" and "Bobo 2") count as one building without the number
    @Query(value = """
        SELECT MIN(b.id) AS buildingId,
               regexp_replace(b.name, '[[:space:]]+[0-9]+$', '') AS buildingName,
               COUNT(DISTINCT x.user_id) AS studentCount,
               COUNT(*) AS gapCount,
               COALESCE(SUM(x.gap_minutes), 0) AS totalGapMinutes
        FROM (
            SELECT DISTINCT ON (g.id)
                   g.id, g.user_id, l.building_id,
                   EXTRACT(EPOCH FROM (g.end_time - g.start_time)) / 60.0 AS gap_minutes
            FROM gaps g
            JOIN location_logs l
              ON l.user_id = g.user_id
             AND l.timestamp BETWEEN g.start_time AND g.end_time
            ORDER BY g.id, l.timestamp DESC
        ) x
        JOIN buildings b ON b.id = x.building_id
        GROUP BY regexp_replace(b.name, '[[:space:]]+[0-9]+$', '')
        ORDER BY studentCount DESC, totalGapMinutes DESC
        """, nativeQuery = true)
    List<BuildingGapPresenceProjection> findGapPresenceByBuilding();


    // BQ 2 TYPE 1
    // A location log is only created when the student is inside a campus building, so each log is one location
    // update received while inside a building. The updates are grouped per "student-hour" (one student in one clock
    // hour with at least one update) and these queries summarize how many updates each student-hour has

    // Average, median and maximum updates per student-hour since :since, and how many student-hours and students
    @Query(value = """
        SELECT CAST(AVG(per_hour.updates) AS double precision) AS "averagePerHour",
            CAST(percentile_cont(0.5) WITHIN GROUP (ORDER BY per_hour.updates) AS double precision) AS "medianPerHour",
            CAST(MAX(per_hour.updates) AS bigint) AS "maxPerHour",
            CAST(COUNT(*) AS bigint) AS "studentHours",
            CAST(COUNT(DISTINCT per_hour.user_id) AS bigint) AS "students"
        FROM (
            SELECT l.user_id, date_trunc('hour', l."timestamp") AS hour_slot, COUNT(*) AS updates
            FROM location_logs l
            WHERE l."timestamp" >= :since
            GROUP BY l.user_id, date_trunc('hour', l."timestamp")
        ) per_hour
        """, nativeQuery = true)
    LocationUpdateSummaryProjection findLocationUpdateSummary(@Param("since") LocalDateTime since);

    // Number of student-hours since :since per range of updates in the hour
    // Ranges without student-hours do not appear in the result (the service adds them with 0)
    @Query(value = """
        SELECT CASE
                WHEN per_hour.updates = 1 THEN '1'
                WHEN per_hour.updates <= 5 THEN '2-5'
                WHEN per_hour.updates <= 15 THEN '6-15'
                WHEN per_hour.updates <= 30 THEN '16-30'
                ELSE '31+'
            END AS "updatesRange",
            CAST(COUNT(*) AS bigint) AS "studentHours"
        FROM (
            SELECT l.user_id, date_trunc('hour', l."timestamp") AS hour_slot, COUNT(*) AS updates
            FROM location_logs l
            WHERE l."timestamp" >= :since
            GROUP BY l.user_id, date_trunc('hour', l."timestamp")
        ) per_hour
        GROUP BY 1
        """, nativeQuery = true)
    List<LocationUpdateRangeProjection> countStudentHoursByUpdatesRange(@Param("since") LocalDateTime since);

    // Average and maximum updates per hour of each student since :since, highest average first
    @Query(value = """
        SELECT u.id AS "userId",
            u.name AS "userName",
            CAST(AVG(per_hour.updates) AS double precision) AS "averagePerHour",
            CAST(MAX(per_hour.updates) AS bigint) AS "maxPerHour",
            CAST(COUNT(*) AS bigint) AS "hoursInside"
        FROM (
            SELECT l.user_id, date_trunc('hour', l."timestamp") AS hour_slot, COUNT(*) AS updates
            FROM location_logs l
            WHERE l."timestamp" >= :since
            GROUP BY l.user_id, date_trunc('hour', l."timestamp")
        ) per_hour
        JOIN users u ON u.id = per_hour.user_id
        GROUP BY u.id, u.name
        ORDER BY "averagePerHour" DESC, "hoursInside" DESC
        """, nativeQuery = true)
    List<StudentLocationUpdateRateProjection> findLocationUpdateRateByStudent(@Param("since") LocalDateTime since);
}
