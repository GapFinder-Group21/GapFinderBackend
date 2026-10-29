package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.InterestModel;
import com.backend.gapfinder.model.repositories.projections.InterestDemandGapProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InterestRepository extends JpaRepository<InterestModel, Long> {

    boolean existsByName(String name);

    // BQ 14: Which interests have the most students but the fewest open tables?
    // Returns all interests with their student count and open table count since
    // :since.
    // The LEFT JOINs keep interests with no students or no open tables.
    // Sorted: most students first, then fewest open tables (highest unmet demand
    // first).
    @Query(value = """
            SELECT i.id AS "interestId",
                i.name AS "interestName",
                CAST(COUNT(DISTINCT ui.user_id) AS bigint) AS "studentCount",
                CAST(COUNT(DISTINCT o.id) AS bigint) AS "openTableCount"
            FROM interests i
            LEFT JOIN user_interests ui ON ui.interest_id = i.id
            LEFT JOIN activities a ON a.interest_id = i.id
            LEFT JOIN open_tables o
                ON o.activity_id = a.id
                AND o.created_at >= :since
            GROUP BY i.id, i.name
            ORDER BY i.id
            """, nativeQuery = true)
    List<InterestDemandGapProjection> findInterestDemandGapSince(@Param("since") LocalDateTime since);
}
