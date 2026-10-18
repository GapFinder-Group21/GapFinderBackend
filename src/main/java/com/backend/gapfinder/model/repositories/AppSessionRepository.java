package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.AppSessionModel;
import com.backend.gapfinder.model.repositories.projections.AppOpenDelayProjection;
import com.backend.gapfinder.model.repositories.projections.AppOpenDelayRangeProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Repository for the openings of the app
@Repository
public interface AppSessionRepository extends JpaRepository<AppSessionModel, Long> {

    // BQ 4 INDIVIDUAL
    // Number of finished gaps starting since :since per range of minutes between the gap start and the FIRST app opening
    // The LEFT JOIN keeps the gaps without any opening (MIN is null), which fall in the 'No abrió' range
    // Ranges without gaps do not appear in the result (the service adds them with 0)
    @Query(value = """
        SELECT CASE
                WHEN per_gap.delay_minutes IS NULL THEN 'No abrió'
                WHEN per_gap.delay_minutes < 5 THEN '0-5'
                WHEN per_gap.delay_minutes < 15 THEN '5-15'
                WHEN per_gap.delay_minutes < 30 THEN '15-30'
                ELSE '30+'
            END AS "delayRange",
            CAST(COUNT(*) AS bigint) AS "gaps"
        FROM (
            SELECT g.id,
                EXTRACT(EPOCH FROM (MIN(s.opened_at) - g.start_time)) / 60 AS delay_minutes
            FROM gaps g
            LEFT JOIN app_sessions s
                ON s.user_id = g.user_id
                AND s.opened_at >= g.start_time
                AND s.opened_at <= g.end_time
            WHERE g.end_time <= LOCALTIMESTAMP
              AND g.start_time >= :since
            GROUP BY g.id, g.start_time
        ) per_gap
        GROUP BY 1
        """, nativeQuery = true)
    List<AppOpenDelayRangeProjection> countGapsByOpenDelayRange(@Param("since") LocalDateTime since);

    // BQ 4 INDIVIDUAL
    // Average minutes between the gap start and the first app opening, and gaps counted, for all users and for one user
    // Only the gaps where the app was opened are counted (AVG ignores the nulls); :userId can be null (no user columns)
    @Query(value = """
        SELECT CAST(AVG(per_gap.delay_minutes) AS double precision) AS "globalAverageMinutes",
            CAST(COUNT(per_gap.delay_minutes) AS bigint) AS "globalGapsCounted",
            CAST(AVG(CASE WHEN per_gap.user_id = CAST(:userId AS bigint) THEN per_gap.delay_minutes END)
                AS double precision) AS "userAverageMinutes",
            CAST(COUNT(CASE WHEN per_gap.user_id = CAST(:userId AS bigint) THEN per_gap.delay_minutes END)
                AS bigint) AS "userGapsCounted"
        FROM (
            SELECT g.id,
                g.user_id,
                EXTRACT(EPOCH FROM (MIN(s.opened_at) - g.start_time)) / 60 AS delay_minutes
            FROM gaps g
            LEFT JOIN app_sessions s
                ON s.user_id = g.user_id
                AND s.opened_at >= g.start_time
                AND s.opened_at <= g.end_time
            WHERE g.end_time <= LOCALTIMESTAMP
              AND g.start_time >= :since
            GROUP BY g.id, g.user_id, g.start_time
        ) per_gap
        """, nativeQuery = true)
    AppOpenDelayProjection findAppOpenDelayAverages(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
