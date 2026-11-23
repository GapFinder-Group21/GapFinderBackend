package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.repositories.ActivityRepository;
import com.backend.gapfinder.model.repositories.AppSessionRepository;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.InterestRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.model.repositories.OpenTableRepository;
import com.backend.gapfinder.model.repositories.UserLocationLogRepository;
import com.backend.gapfinder.model.repositories.projections.RatingByScoreRangeProjection;
import com.backend.gapfinder.view.dto.RatingByScoreRangeDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

// BQ 16: average stars of the match ratings by score range
class AnalyticsServiceRatingByScoreRangeTest {

    private MatchRepository matchRepository;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        service = new AnalyticsService(
                mock(OpenTableAbandonmentService.class), mock(GapRepository.class),
                mock(UserLocationLogRepository.class), matchRepository, mock(OpenTableRepository.class),
                mock(AppSessionRepository.class), mock(UserService.class), mock(ActivityRepository.class),
                mock(InterestRepository.class));
    }

    private static RatingByScoreRangeProjection row(String range, long ratings, double averageStars) {
        RatingByScoreRangeProjection row = mock(RatingByScoreRangeProjection.class);
        when(row.getScoreRange()).thenReturn(range);
        when(row.getRatings()).thenReturn(ratings);
        when(row.getAverageStars()).thenReturn(averageStars);
        return row;
    }

    // The rows are built before the stubbing, because a mock cannot be stubbed inside another stubbing
    private void queryReturns(RatingByScoreRangeProjection... rows) {
        List<RatingByScoreRangeProjection> list = List.of(rows);
        when(matchRepository.countRatingsByScoreRange(any())).thenReturn(list);
    }

    @Test
    void returnsTheFiveRangesInNaturalOrderIncludingTheEmptyOnes() {
        queryReturns(row("100-150", 10, 4.2), row("0-50", 6, 3.0));

        List<RatingByScoreRangeDTO> result = service.getRatingByScoreRange(LocalDateTime.now().minusDays(7));

        assertEquals(List.of("0-50", "50-100", "100-150", "150-200", "200+"),
                result.stream().map(RatingByScoreRangeDTO::range).toList());
        assertEquals(0, result.get(1).ratings());
        assertEquals(0.0, result.get(1).averageStars()); // empty range: 0, not a missing value
        assertFalse(result.get(1).bestRated());
    }

    @Test
    void returnsTheNumberOfRatingsAndTheAverageStarsOfEachRange() {
        queryReturns(row("0-50", 6, 3.0), row("50-100", 8, 3.5), row("150-200", 12, 4.25), row("200+", 20, 4.6));

        List<RatingByScoreRangeDTO> result = service.getRatingByScoreRange(null);

        assertEquals(6, result.get(0).ratings());
        assertEquals(3.0, result.get(0).averageStars());
        assertEquals(8, result.get(1).ratings());
        assertEquals(3.5, result.get(1).averageStars());
        assertEquals(12, result.get(3).ratings());
        assertEquals(4.25, result.get(3).averageStars());
        assertEquals(20, result.get(4).ratings());
        assertEquals(4.6, result.get(4).averageStars());
    }

    @Test
    void marksOnlyTheRangeWithTheHighestAverageStars() {
        queryReturns(row("0-50", 6, 3.0), row("50-100", 8, 4.4), row("100-150", 10, 4.1), row("200+", 7, 3.9));

        List<RatingByScoreRangeDTO> result = service.getRatingByScoreRange(null);

        assertEquals(List.of("50-100"), result.stream().filter(RatingByScoreRangeDTO::bestRated)
                .map(RatingByScoreRangeDTO::range).toList());
    }

    @Test
    void theFirstRangeWinsATie() {
        queryReturns(row("50-100", 10, 4.0), row("150-200", 6, 4.0));

        List<RatingByScoreRangeDTO> result = service.getRatingByScoreRange(null);

        assertTrue(result.get(1).bestRated());
        assertFalse(result.get(3).bestRated());
    }

    @Test
    void aRangeWithFewerThanFiveRatingsIsNotHighlightedEvenWithTheHighestAverage() {
        queryReturns(row("0-50", 1, 5.0), row("100-150", 50, 4.0));

        List<RatingByScoreRangeDTO> result = service.getRatingByScoreRange(null);

        assertEquals(5.0, result.get(0).averageStars());
        assertFalse(result.get(0).bestRated());
        assertTrue(result.get(2).bestRated());
    }

    @Test
    void aRangeWithExactlyFiveRatingsCanBeHighlighted() {
        queryReturns(row("0-50", 5, 5.0), row("100-150", 50, 4.0));

        List<RatingByScoreRangeDTO> result = service.getRatingByScoreRange(null);

        assertTrue(result.get(0).bestRated());
        assertFalse(result.get(2).bestRated());
    }

    @Test
    void marksNoRangeWhenAllHaveFewerThanFiveRatings() {
        queryReturns(row("0-50", 1, 5.0), row("50-100", 4, 4.5));

        List<RatingByScoreRangeDTO> result = service.getRatingByScoreRange(null);

        assertTrue(result.stream().noneMatch(RatingByScoreRangeDTO::bestRated));
    }

    @Test
    void throwsNotFoundWhenThereAreNoRatings() {
        queryReturns();

        assertThrows(NotFoundException.class, () -> service.getRatingByScoreRange(null));
    }

    @Test
    void usesTheGivenStartDate() {
        queryReturns(row("0-50", 6, 3.0));
        LocalDateTime since = LocalDateTime.of(2026, 9, 1, 0, 0);

        service.getRatingByScoreRange(since);

        verify(matchRepository).countRatingsByScoreRange(since);
    }

    @Test
    void usesTheLastThirtyDaysWhenThereIsNoStartDate() {
        queryReturns(row("0-50", 6, 3.0));
        LocalDateTime before = LocalDateTime.now().minusDays(30);

        service.getRatingByScoreRange(null);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(matchRepository).countRatingsByScoreRange(captor.capture());
        LocalDateTime after = LocalDateTime.now().minusDays(30);
        assertFalse(captor.getValue().isBefore(before));
        assertFalse(captor.getValue().isAfter(after));
    }
}
