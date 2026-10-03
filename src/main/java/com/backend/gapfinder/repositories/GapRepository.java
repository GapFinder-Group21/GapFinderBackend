package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.GapModel;
import com.backend.gapfinder.repositories.projections.SharedInterestGroupProjection;
import com.backend.gapfinder.repositories.projections.UnmatchedFreeTimeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GapRepository extends JpaRepository<GapModel, Long> {

    // Finds potential gaps that overlap in time, belong to other users, and are NOT already in an ACCEPTED or COMPLETED match
    @Query("SELECT g FROM GapModel g WHERE g.user.id <> :userId " +
           "AND g.startTime < :endTime AND g.endTime > :startTime " +
           "AND NOT EXISTS (SELECT m FROM MatchModel m WHERE (m.proposerGap = g OR m.acceptorGap = g) AND m.status = com.backend.gapfinder.enums.MatchStatusEnum.ACCEPTED)")
    List<GapModel> findOverlappingGapsExcludingUser(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Coverage per gap duration bucket: gap count, total gap minutes, total matched minutes
    // Only gaps that already ended are counted: matches happen in real time, so a future gap cannot have one yet
    @Query(value = """
        SELECT per_gap.duration_range,
            COUNT(*) AS gap_count,
            SUM(per_gap.gap_minutes) AS total_gap_minutes,
            SUM(per_gap.covered_minutes) AS total_covered_minutes
        FROM (
            SELECT g.id,
                EXTRACT(EPOCH FROM (g.end_time - g.start_time)) / 60 AS gap_minutes,
                LEAST(
                    COALESCE(SUM(EXTRACT(EPOCH FROM (m.end_time - m.start_time)) / 60), 0),
                    EXTRACT(EPOCH FROM (g.end_time - g.start_time)) / 60
                ) AS covered_minutes,
                CASE
                    WHEN EXTRACT(EPOCH FROM (g.end_time - g.start_time)) / 60 < 30 THEN '<30'
                    WHEN EXTRACT(EPOCH FROM (g.end_time - g.start_time)) / 60 < 60 THEN '30-60'
                    WHEN EXTRACT(EPOCH FROM (g.end_time - g.start_time)) / 60 < 120 THEN '60-120'
                    WHEN EXTRACT(EPOCH FROM (g.end_time - g.start_time)) / 60 < 180 THEN '120-180'
                    ELSE '180+'
                END AS duration_range
            FROM gaps g
            LEFT JOIN matches m
                ON (m.proposer_gap_id = g.id OR m.acceptor_gap_id = g.id)
                AND m.status IN ('ACCEPTED', 'COMPLETED')
            WHERE g.end_time <= LOCALTIMESTAMP
            GROUP BY g.id
        ) per_gap
        GROUP BY per_gap.duration_range
        """, nativeQuery = true)
    List<Object[]> findCoverageByDurationBucket();

    // Gaps of a user that start inside the given range [from, to)
    @Query("SELECT g FROM GapModel g WHERE g.user.id = :userId " +
           "AND g.startTime >= :from AND g.startTime < :to")
    List<GapModel> findByUserAndStartBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // BQ 12 INDIVIDUAL
    // Free time and matched minutes per (career, semester) for the gaps between [from, to]
    // A gap's matched minutes are capped at its own duration (same rule as findCoverageByDurationBucket)
    @Query(value = """
        SELECT u.career AS "career",
            u.semester AS "semester",
            CAST(COUNT(DISTINCT u.id) AS bigint) AS "students",
            CAST(COUNT(*) AS bigint) AS "totalGaps",
            CAST(SUM(CASE WHEN per_gap.match_count = 0 THEN 1 ELSE 0 END) AS bigint) AS "unmatchedGaps",
            CAST(SUM(per_gap.gap_minutes) AS double precision) AS "totalGapMinutes",
            CAST(SUM(per_gap.covered_minutes) AS double precision) AS "matchedMinutes"
        FROM (
            SELECT g.id,
                g.user_id,
                COUNT(m.id) AS match_count,
                EXTRACT(EPOCH FROM (g.end_time - g.start_time)) / 60 AS gap_minutes,
                LEAST(
                    COALESCE(SUM(EXTRACT(EPOCH FROM (m.end_time - m.start_time)) / 60), 0),
                    EXTRACT(EPOCH FROM (g.end_time - g.start_time)) / 60
                ) AS covered_minutes
            FROM gaps g
            LEFT JOIN matches m
                ON (m.proposer_gap_id = g.id OR m.acceptor_gap_id = g.id)
                AND m.status IN ('ACCEPTED', 'COMPLETED')
            WHERE g.start_time >= :from
              AND g.end_time <= :to
            GROUP BY g.id
        ) per_gap
        JOIN users u ON u.id = per_gap.user_id
        GROUP BY u.career, u.semester
        """, nativeQuery = true)
    List<UnmatchedFreeTimeProjection> findFreeTimeByCareerAndSemester(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // BQ 13 INDIVIDUAL
    // Largest group of students sharing each interest who are free at the same time, for the gaps starting in [from, to)
    // The candidate moments are the gap starts: at each one, count the distinct students with the interest whose gap covers it
    @Query(value = """
        SELECT ranked."interestId",
            ranked."interestName",
            ranked."largestGroupSize",
            ranked."peakTime",
            (SELECT CAST(COUNT(*) AS bigint) FROM user_interests x
             WHERE x.interest_id = ranked."interestId") AS "studentsWithInterest"
        FROM (
            SELECT i.id AS "interestId",
                i.name AS "interestName",
                CAST(COUNT(DISTINCT g2.user_id) AS bigint) AS "largestGroupSize",
                g.start_time AS "peakTime",
                ROW_NUMBER() OVER (
                    PARTITION BY i.id
                    ORDER BY COUNT(DISTINCT g2.user_id) DESC, g.start_time
                ) AS rn
            FROM interests i
            JOIN user_interests ui ON ui.interest_id = i.id
            JOIN gaps g ON g.user_id = ui.user_id
            JOIN gaps g2 ON g2.start_time <= g.start_time AND g2.end_time > g.start_time
            JOIN user_interests ui2 ON ui2.user_id = g2.user_id AND ui2.interest_id = i.id
            WHERE g.start_time >= :from
              AND g.start_time < :to
            GROUP BY i.id, i.name, g.start_time
        ) ranked
        WHERE ranked.rn = 1
        """, nativeQuery = true)
    List<SharedInterestGroupProjection> findLargestFreeGroupByInterest(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


    }