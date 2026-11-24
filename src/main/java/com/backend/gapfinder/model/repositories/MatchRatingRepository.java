package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.MatchRatingModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRatingRepository extends JpaRepository<MatchRatingModel, Long> {

    // The rating a user gave to a match, if any
    Optional<MatchRatingModel> findByMatchIdAndRaterId(Long matchId, Long raterId);

    // All the ratings of several matches in one query (used by the history)
    List<MatchRatingModel> findByMatchIdIn(Collection<Long> matchIds);
}
