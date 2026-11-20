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
import com.backend.gapfinder.model.repositories.projections.StudyModeByGapDurationProjection;
import com.backend.gapfinder.view.dto.StudyModeByGapDurationDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

// BQ 15: share of the finished gaps of each duration range that have study mode
class AnalyticsServiceStudyModeByGapDurationTest {

    private GapRepository gapRepository;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        gapRepository = mock(GapRepository.class);
        service = new AnalyticsService(
                mock(OpenTableAbandonmentService.class), gapRepository,
                mock(UserLocationLogRepository.class), mock(MatchRepository.class), mock(OpenTableRepository.class),
                mock(AppSessionRepository.class), mock(UserService.class), mock(ActivityRepository.class),
                mock(InterestRepository.class));
    }

    private static StudyModeByGapDurationProjection row(String range, long total, long withStudyMode) {
        StudyModeByGapDurationProjection row = mock(StudyModeByGapDurationProjection.class);
        when(row.getDurationRange()).thenReturn(range);
        when(row.getTotal()).thenReturn(total);
        when(row.getWithStudyMode()).thenReturn(withStudyMode);
        return row;
    }

    // The rows are built before the stubbing, because a mock cannot be stubbed inside another stubbing
    private void queryReturns(StudyModeByGapDurationProjection... rows) {
        List<StudyModeByGapDurationProjection> list = List.of(rows);
        when(gapRepository.countStudyModeByGapDuration()).thenReturn(list);
    }

    private static StudyModeByGapDurationDTO of(List<StudyModeByGapDurationDTO> result, String range) {
        return result.stream().filter(stat -> stat.range().equals(range)).findFirst().orElseThrow();
    }

    @Test
    void returnsTheFourRangesInOrderIncludingTheOnesThatDoNotAppear() {
        // The query leaves out the ranges without finished gaps, and the rows come in any order
        queryReturns(row("180+", 10, 4), row("60-120", 20, 2));

        List<StudyModeByGapDurationDTO> result = service.getStudyModeByGapDuration();

        assertEquals(List.of("<60", "60-120", "120-180", "180+"),
                result.stream().map(StudyModeByGapDurationDTO::range).toList());
        assertEquals(0, result.get(0).totalGaps());
        assertEquals(0, result.get(0).gapsWithStudyMode());
        assertEquals(0, result.get(2).totalGaps());
    }

    @Test
    void calculatesThePercentageOfEachRange() {
        queryReturns(row("<60", 50, 4), row("60-120", 40, 6), row("120-180", 20, 5), row("180+", 8, 4));

        List<StudyModeByGapDurationDTO> result = service.getStudyModeByGapDuration();

        assertEquals(8.0, of(result, "<60").percentage());
        assertEquals(15.0, of(result, "60-120").percentage());
        assertEquals(25.0, of(result, "120-180").percentage());
        assertEquals(50.0, of(result, "180+").percentage());
        assertEquals(40, of(result, "60-120").totalGaps());
        assertEquals(6, of(result, "60-120").gapsWithStudyMode());
    }

    @Test
    void aRangeWithoutGapsIsZeroPercentWithoutDividingByZero() {
        queryReturns(row("60-120", 10, 5));

        List<StudyModeByGapDurationDTO> result = service.getStudyModeByGapDuration();

        assertEquals(0.0, of(result, "<60").percentage()); // no gaps in the range: 0 % and no error
        assertEquals(0.0, of(result, "180+").percentage());
        assertEquals(50.0, of(result, "60-120").percentage());
    }

    @Test
    void aRangeWhereNoGapHasStudyModeIsZeroPercent() {
        queryReturns(row("<60", 30, 0), row("60-120", 10, 2));

        List<StudyModeByGapDurationDTO> result = service.getStudyModeByGapDuration();

        assertEquals(0.0, of(result, "<60").percentage());
        assertEquals(20.0, of(result, "60-120").percentage());
    }

    @Test
    void onlyTheRangeWithTheHighestPercentageIsMarked() {
        queryReturns(row("<60", 50, 4), row("60-120", 40, 6), row("120-180", 20, 5), row("180+", 8, 2));

        List<StudyModeByGapDurationDTO> result = service.getStudyModeByGapDuration();

        // 8 %, 15 %, 25 % and 25 %: the highest is 25 % and the first one in order wins
        assertEquals(List.of("120-180"), result.stream().filter(StudyModeByGapDurationDTO::highestUse)
                .map(StudyModeByGapDurationDTO::range).toList());
    }

    @Test
    void theHighestIsTheOneWithTheBiggestPercentageNotTheOneWithMostGaps() {
        // The range with most gaps with study mode (30) is not the one with the highest share
        queryReturns(row("<60", 300, 30), row("180+", 4, 2));

        List<StudyModeByGapDurationDTO> result = service.getStudyModeByGapDuration();

        assertTrue(of(result, "180+").highestUse());
        assertFalse(of(result, "<60").highestUse());
    }

    @Test
    void onATieTheFirstRangeInOrderIsTheOneMarked() {
        queryReturns(row("<60", 10, 2), row("60-120", 10, 2), row("180+", 5, 1));

        List<StudyModeByGapDurationDTO> result = service.getStudyModeByGapDuration();

        // 20 %, 20 % and 20 %: only one is marked, and it is always the same one
        assertTrue(of(result, "<60").highestUse());
        assertEquals(1, result.stream().filter(StudyModeByGapDurationDTO::highestUse).count());
    }

    @Test
    void ifNoGapHasStudyModeNoRangeIsMarked() {
        queryReturns(row("<60", 30, 0), row("60-120", 10, 0));

        List<StudyModeByGapDurationDTO> result = service.getStudyModeByGapDuration();

        assertEquals(4, result.size());
        assertTrue(result.stream().noneMatch(StudyModeByGapDurationDTO::highestUse));
    }

    @Test
    void nullCountsOfTheQueryAreZero() {
        StudyModeByGapDurationProjection nulls = mock(StudyModeByGapDurationProjection.class);
        when(nulls.getDurationRange()).thenReturn("<60");
        when(nulls.getTotal()).thenReturn(null);
        when(nulls.getWithStudyMode()).thenReturn(null);
        queryReturns(nulls, row("60-120", 10, 1));

        List<StudyModeByGapDurationDTO> result = service.getStudyModeByGapDuration();

        assertEquals(0, of(result, "<60").totalGaps());
        assertEquals(0.0, of(result, "<60").percentage());
    }

    @Test
    void throwsNotFoundWhenThereAreNoFinishedGaps() {
        queryReturns();

        assertThrows(NotFoundException.class, () -> service.getStudyModeByGapDuration());
    }

    @Test
    void throwsNotFoundWhenEveryRangeHasZeroGaps() {
        queryReturns(row("<60", 0, 0), row("180+", 0, 0));

        assertThrows(NotFoundException.class, () -> service.getStudyModeByGapDuration());
    }
}
