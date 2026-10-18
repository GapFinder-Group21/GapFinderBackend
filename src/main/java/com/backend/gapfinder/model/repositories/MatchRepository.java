package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.enums.MatchStatusEnum;
import com.backend.gapfinder.model.models.MatchModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Check if a gap is used by any match, as proposer or acceptor
    boolean existsByProposerGapIdOrAcceptorGapId(Long proposerGapId, Long acceptorGapId);

    // Count the matches with a given status (BQ 7)
    long countByStatus(MatchStatusEnum status);
}