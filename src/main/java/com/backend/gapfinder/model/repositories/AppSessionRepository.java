package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.AppSessionModel;
import com.backend.gapfinder.model.repositories.projections.AppOpenDelayProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Repository for the openings of the app
@Repository
public interface AppSessionRepository extends JpaRepository<AppSessionModel, Long> {

    // BQ 4 INDIVIDUAL
    // Minutes between the start of each finished gap and the FIRST app opening inside it (gaps without openings are left out)
    // Returns one row: average and count for the given user, and average and count for all users
    @Query(value = """
        SELECT CAST(AVG(CASE WHEN per_gap.user_id = :userId THEN per_gap.delay_minutes END) AS double precision) AS "userAverageMinutes",
            CAST(COUNT(CASE WHEN per_gap.user_id = :userId THEN 1 END) AS bigint) AS "userGapsCounted",
            CAST(AVG(per_gap.delay_minutes) AS double precision) AS "globalAverageMinutes",
            CAST(COUNT(*) AS bigint) AS "globalGapsCounted"
        FROM (
            SELECT g.id,
                g.user_id,
                EXTRACT(EPOCH FROM (MIN(s.opened_at) - g.start_time)) / 60 AS delay_minutes
            FROM gaps g
            JOIN app_sessions s
                ON s.user_id = g.user_id
                AND s.opened_at >= g.start_time
                AND s.opened_at <= g.end_time
            WHERE g.end_time <= LOCALTIMESTAMP
            GROUP BY g.id, g.user_id, g.start_time
        ) per_gap
        """, nativeQuery = true)
    AppOpenDelayProjection findAppOpenDelay(@Param("userId") Long userId);
}
