package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.backend.gapfinder.model.repositories.projections.RejectionByScoreRangeProjection;
import com.backend.gapfinder.view.dto.RejectionByScoreRangeDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

// BQ 10: rejection rate by score range
class AnalyticsServiceRejectionTest {

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

    private static RejectionByScoreRangeProjection row(String range, long total, long rejected) {
        RejectionByScoreRangeProjection row = mock(RejectionByScoreRangeProjection.class);
        when(row.getScoreRange()).thenReturn(range);
        when(row.getTotal()).thenReturn(total);
        when(row.getRejected()).thenReturn(rejected);
        return row;
    }

    // The rows are built before the stubbing, because a mock cannot be stubbed inside another stubbing
    private void queryReturns(RejectionByScoreRangeProjection... rows) {
        List<RejectionByScoreRangeProjection> list = List.of(rows);
        when(matchRepository.countRejectionsByScoreRange(any())).thenReturn(list);
    }

    @Test
    void returnsTheFiveRangesInNaturalOrderIncludingTheEmptyOnes() {
        queryReturns(row("100-150", 10, 4), row("0-50", 4, 1));

        List<RejectionByScoreRangeDTO> result = service.getRejectionByScoreRange(LocalDateTime.now().minusDays(7));

        assertEquals(List.of("0-50", "50-100", "100-150", "150-200", "200+"),
                result.stream().map(RejectionByScoreRangeDTO::range).toList());
        assertEquals(0, result.get(1).total());
        assertEquals(0.0, result.get(1).rejectionPercentage()); // empty range: no division by zero
        assertEquals(25.0, result.get(0).rejectionPercentage());
        assertEquals(40.0, result.get(2).rejectionPercentage());
    }

    @Test
    void marksOnlyTheRangeWithTheHighestRejectionRate() {
        queryReturns(row("0-50", 4, 1), row("50-100", 10, 5), row("100-150", 10, 4), row("200+", 2, 0));

        List<RejectionByScoreRangeDTO> result = service.getRejectionByScoreRange(null);

        assertEquals(List.of("50-100"), result.stream().filter(RejectionByScoreRangeDTO::highestRejection)
                .map(RejectionByScoreRangeDTO::range).toList());
    }

    @Test
    void theFirstRangeWinsATie() {
        queryReturns(row("50-100", 10, 5), row("150-200", 4, 2));

        List<RejectionByScoreRangeDTO> result = service.getRejectionByScoreRange(null);

        assertTrue(result.get(1).highestRejection());
        assertFalse(result.get(3).highestRejection());
    }

    @Test
    void marksNoRangeWhenNothingWasRejected() {
        queryReturns(row("0-50", 3, 0));

        List<RejectionByScoreRangeDTO> result = service.getRejectionByScoreRange(null);

        assertTrue(result.stream().noneMatch(RejectionByScoreRangeDTO::highestRejection));
    }

    @Test
    void throwsNotFoundWhenThereAreNoAnsweredMatches() {
        queryReturns();

        assertThrows(NotFoundException.class, () -> service.getRejectionByScoreRange(null));
    }
}
