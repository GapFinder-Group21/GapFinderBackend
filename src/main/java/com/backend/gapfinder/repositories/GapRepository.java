package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.GapModel;
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


    }