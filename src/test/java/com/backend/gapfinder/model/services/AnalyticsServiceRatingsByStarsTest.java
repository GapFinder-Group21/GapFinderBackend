package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.repositories.ActivityRepository;
import com.backend.gapfinder.model.repositories.AppSessionRepository;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.InterestRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.model.repositories.OpenTableRepository;
import com.backend.gapfinder.model.repositories.UserLocationLogRepository;
import com.backend.gapfinder.model.repositories.projections.RatingStarsProjection;
import com.backend.gapfinder.view.dto.RatingStarsDistributionDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

// BQ 13: share of the match ratings with 1, 2, 3, 4 and 5 stars
class AnalyticsServiceRatingsByStarsTest {

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

    private static RatingStarsProjection row(int stars, long ratings) {
        RatingStarsProjection row = mock(RatingStarsProjection.class);
        when(row.getStars()).thenReturn(stars);
        when(row.getRatings()).thenReturn(ratings);
        return row;
    }

    // The rows are built before the stubbing, because a mock cannot be stubbed inside another stubbing
    private void queryReturns(RatingStarsProjection... rows) {
        List<RatingStarsProjection> list = List.of(rows);
        when(matchRepository.countRatingsByStars()).thenReturn(list);
    }

    @Test
    void returnsTheFiveNumbersOfStarsInOrderIncludingTheEmptyOnes() {
        queryReturns(row(5, 6), row(3, 2));

        List<RatingStarsDistributionDTO> result = service.getRatingsByStars();

        assertEquals(List.of(1, 2, 3, 4, 5), result.stream().map(RatingStarsDistributionDTO::stars).toList());
        assertEquals(0, result.get(0).ratings());
        assertEquals(0.0, result.get(0).percentage()); // nobody gave 1 star: 0 % and no error
        assertEquals(0.0, result.get(3).percentage());
    }

    @Test
    void calculatesThePercentageOfEachNumberOfStars() {
        queryReturns(row(1, 1), row(2, 1), row(3, 2), row(4, 4), row(5, 2));

        List<RatingStarsDistributionDTO> result = service.getRatingsByStars();

        assertEquals(10.0, result.get(0).percentage());
        assertEquals(10.0, result.get(1).percentage());
        assertEquals(20.0, result.get(2).percentage());
        assertEquals(40.0, result.get(3).percentage());
        assertEquals(20.0, result.get(4).percentage());
        assertEquals(100.0, result.stream().mapToDouble(RatingStarsDistributionDTO::percentage).sum(), 0.0001);
    }

    @Test
    void marksOnlyTheMostCommonNumberOfStars() {
        queryReturns(row(2, 1), row(4, 5), row(5, 3));

        List<RatingStarsDistributionDTO> result = service.getRatingsByStars();

        assertEquals(1, result.stream().filter(RatingStarsDistributionDTO::mostCommon).count());
        assertTrue(result.get(3).mostCommon());
        assertFalse(result.get(4).mostCommon());
    }

    @Test
    void onATieTheOneWithFewerStarsIsMarked() {
        queryReturns(row(3, 4), row(5, 4));

        List<RatingStarsDistributionDTO> result = service.getRatingsByStars();

        assertTrue(result.get(2).mostCommon());
        assertFalse(result.get(4).mostCommon());
    }

    @Test
    void anEmptyNumberOfStarsIsNeverMarked() {
        queryReturns(row(5, 1));

        List<RatingStarsDistributionDTO> result = service.getRatingsByStars();

        assertFalse(result.get(0).mostCommon()); // 1 star has 0 ratings, so it cannot win
        assertTrue(result.get(4).mostCommon());
    }

    @Test
    void failsWhenThereAreNoRatings() {
        queryReturns();

        assertThrows(NotFoundException.class, () -> service.getRatingsByStars());
    }
}
