package com.backend.gapfinder.repositories;

import com.backend.gapfinder.enums.MatchStatusEnum;
import com.backend.gapfinder.models.MatchModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<MatchModel, Long> {

    // Get all matches with a given status (used to auto-complete expired ones)
    List<MatchModel> findByStatus(MatchStatusEnum status);

    // Get the matches with a given status whose acceptor gap belongs to the given user
    List<MatchModel> findByAcceptorGapUserIdAndStatus(Long userId, MatchStatusEnum status);

    // Check if a gap is used by any match, as proposer or acceptor
    boolean existsByProposerGapIdOrAcceptorGapId(Long proposerGapId, Long acceptorGapId);
}