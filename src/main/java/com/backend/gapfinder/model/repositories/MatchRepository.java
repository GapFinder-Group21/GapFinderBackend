package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.enums.MatchStatusEnum;
import com.backend.gapfinder.model.models.MatchModel;
import com.backend.gapfinder.model.repositories.projections.RejectionByScoreRangeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MatchRepository extends JpaRepository<MatchModel, Long> {

    // Check if a gap is used, as proposer or acceptor, by a match in any of the given statuses
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM MatchModel m
        WHERE (m.proposerGap.id = :gapId OR m.acceptorGap.id = :gapId)
          AND m.status IN :statuses
        """)
    boolean existsByGapAndStatusIn(@Param("gapId") Long gapId,
                                   @Param("statuses") Collection<MatchStatusEnum> statuses);

    // Check if two gaps already have a match between them, in either direction, in any of the given statuses
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM MatchModel m
        WHERE ((m.proposerGap.id = :gapA AND m.acceptorGap.id = :gapB)
            OR (m.proposerGap.id = :gapB AND m.acceptorGap.id = :gapA))
          AND m.status IN :statuses
        """)
    boolean existsBetweenGapsAndStatusIn(@Param("gapA") Long gapA,
                                         @Param("gapB") Long gapB,
                                         @Param("statuses") Collection<MatchStatusEnum> statuses);

    // Get all matches with a given status (used to auto-complete expired ones)
    List<MatchModel> findByStatus(MatchStatusEnum status);

    // Get the matches with a given status whose acceptor gap belongs to the given user
    List<MatchModel> findByAcceptorGapUserIdAndStatus(Long userId, MatchStatusEnum status);

    // Get the matches of a user, as proposer or as acceptor
    List<MatchModel> findByProposerGapUserIdOrAcceptorGapUserId(Long proposerUserId, Long acceptorUserId);

    // Check if a gap is used by any match, as proposer or acceptor
    boolean existsByProposerGapIdOrAcceptorGapId(Long proposerGapId, Long acceptorGapId);

    // Count the matches with a given status (BQ 7)
    long countByStatus(MatchStatusEnum status);

    // BQ 10 INDIVIDUAL TYPE 3
    // Answered matches (ACCEPTED, REJECTED or COMPLETED) starting since :since per range of score, with how many were rejected
    // The score is minutes of overlap (1 point per minute) plus bonuses of at most 35, so the ranges go up by 50 points
    // A match that is still PENDING has no answer yet, and a null score has no range, so both are left out
    // Ranges without matches do not appear in the result (the service adds them with 0)
    @Query(value = """
        SELECT CASE
                WHEN m.score < 50 THEN '0-50'
                WHEN m.score < 100 THEN '50-100'
                WHEN m.score < 150 THEN '100-150'
                WHEN m.score < 200 THEN '150-200'
                ELSE '200+'
            END AS "scoreRange",
            CAST(COUNT(*) AS bigint) AS "total",
            CAST(SUM(CASE WHEN m.status = 'REJECTED' THEN 1 ELSE 0 END) AS bigint) AS "rejected"
        FROM matches m
        WHERE m.status IN ('ACCEPTED', 'REJECTED', 'COMPLETED')
          AND m.score IS NOT NULL
          AND m.start_time >= :since
        GROUP BY 1
        """, nativeQuery = true)
    List<RejectionByScoreRangeProjection> countRejectionsByScoreRange(@Param("since") LocalDateTime since);
}