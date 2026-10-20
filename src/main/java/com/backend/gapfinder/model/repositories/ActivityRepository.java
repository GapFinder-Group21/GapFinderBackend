package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.ActivityModel;
import com.backend.gapfinder.model.repositories.projections.UnusedActivityProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityRepository extends JpaRepository<ActivityModel, Long> {

    // BQ 8 INDIVIDUAL
    // ALL the activities with the open tables created since :since, from the least to the most chosen
    // The LEFT JOIN keeps the activities without any open table (COUNT of a null id is 0), which are the NEVER ones
    // The date goes in the ON and not in the WHERE: a WHERE would drop the activities with no tables in the period
    // Open tables without activity (activity_id null) never match the join, so they are not counted
    @Query(value = """
        SELECT a.id AS "activityId",
            a.name AS "activityName",
            i.name AS "interestName",
            CAST(COUNT(o.id) AS bigint) AS "openTables"
        FROM activities a
        JOIN interests i ON i.id = a.interest_id
        LEFT JOIN open_tables o
            ON o.activity_id = a.id
            AND o.created_at >= :since
        GROUP BY a.id, a.name, i.name
        ORDER BY COUNT(o.id) ASC, a.name ASC
        """, nativeQuery = true)
    List<UnusedActivityProjection> findActivityUsageSince(@Param("since") LocalDateTime since);
}
