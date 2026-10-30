package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.backend.gapfinder.model.repositories.projections.PreferredActivityRankingProjection;
import com.backend.gapfinder.model.repositories.projections.PreferredActivityTotalsProjection;
import com.backend.gapfinder.model.repositories.projections.StudentActivityUsageProjection;
import com.backend.gapfinder.view.dto.PreferredActivityStatsDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

// BQ 6 type 2: the service only builds the answer from the query rows, so the repositories are mocked
class AnalyticsServicePreferredActivityTest {

    private static final LocalDateTime SINCE = LocalDateTime.of(2026, 9, 1, 0, 0);

    private OpenTableRepository openTableRepository;
    private UserService userService;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        openTableRepository = mock(OpenTableRepository.class);
        userService = mock(UserService.class);
        service = new AnalyticsService(mock(OpenTableAbandonmentService.class), mock(GapRepository.class),
                mock(UserLocationLogRepository.class), mock(MatchRepository.class), openTableRepository,
                mock(AppSessionRepository.class), userService, mock(ActivityRepository.class),
                mock(InterestRepository.class));
    }

    // The query rows are plain objects (not mocks), so they can be built inside a thenReturn call
    private static PreferredActivityTotalsProjection totals(long tables, long students, long withSeveral, long repeating) {
        return new PreferredActivityTotalsProjection() {
            public Long getTables() { return tables; }
            public Long getStudents() { return students; }
            public Long getStudentsWithSeveralTables() { return withSeveral; }
            public Long getStudentsRepeatingSameActivity() { return repeating; }
        };
    }

    private static PreferredActivityRankingProjection ranking(long id, String name, long tables, long students) {
        return new PreferredActivityRankingProjection() {
            public Long getActivityId() { return id; }
            public String getActivityName() { return name; }
            public Long getTables() { return tables; }
            public Long getStudents() { return students; }
        };
    }

    private static StudentActivityUsageProjection usage(long id, String name, long timesChosen) {
        return new StudentActivityUsageProjection() {
            public Long getActivityId() { return id; }
            public String getActivityName() { return name; }
            public Long getTimesChosen() { return timesChosen; }
        };
    }

    @Test
    void withoutUserItAnswersTheRankingAndTheRepeatPercentage() {
        when(openTableRepository.findPreferredActivityTotals(SINCE)).thenReturn(totals(10, 4, 3, 1));
        when(openTableRepository.countOpenTablesByActivity(SINCE))
                .thenReturn(List.of(ranking(1, "Chess", 6, 3), ranking(2, "Soccer", 4, 2)));

        PreferredActivityStatsDTO result = service.getPreferredActivity(null, SINCE);

        assertEquals(2, result.ranking().size());
        assertEquals("Chess", result.ranking().get(0).activityName());
        assertEquals(60.0, result.ranking().get(0).percentage());
        assertEquals(10, result.totalTables());
        assertEquals(4, result.totalStudents());
        assertEquals(100.0 / 3, result.repeatPercentage(), 0.0001);
        assertNull(result.userPreferredActivity());
        // Without a user nobody is validated and the student query is not used
        verify(userService, org.mockito.Mockito.never()).getById(any());
        verify(openTableRepository, org.mockito.Mockito.never()).findActivityUsageByStudent(any(), any());
    }

    @Test
    void withUserItAddsTheirFirstRowAsPreferredActivity() {
        when(openTableRepository.findPreferredActivityTotals(SINCE)).thenReturn(totals(5, 2, 1, 0));
        when(openTableRepository.countOpenTablesByActivity(SINCE)).thenReturn(List.of(ranking(1, "Chess", 5, 2)));
        // The query already sorts: most chosen first, tie the most recently used
        when(openTableRepository.findActivityUsageByStudent(7L, SINCE))
                .thenReturn(List.of(usage(2, "Soccer", 3), usage(1, "Chess", 2)));

        PreferredActivityStatsDTO result = service.getPreferredActivity(7L, SINCE);

        assertEquals(2L, result.userPreferredActivity().activityId());
        assertEquals("Soccer", result.userPreferredActivity().activityName());
        assertEquals(3, result.userPreferredActivity().timesChosen());
        verify(userService).getById(7L);
    }

    @Test
    void userWithoutOpenTablesInThePeriodHasNoPreferredActivity() {
        when(openTableRepository.findPreferredActivityTotals(SINCE)).thenReturn(totals(5, 2, 1, 0));
        when(openTableRepository.countOpenTablesByActivity(SINCE)).thenReturn(List.of(ranking(1, "Chess", 5, 2)));
        when(openTableRepository.findActivityUsageByStudent(7L, SINCE)).thenReturn(List.of());

        assertNull(service.getPreferredActivity(7L, SINCE).userPreferredActivity());
    }

    @Test
    void repeatPercentageIsNullWhenNoStudentHasSeveralOpenTables() {
        when(openTableRepository.findPreferredActivityTotals(SINCE)).thenReturn(totals(2, 2, 0, 0));
        when(openTableRepository.countOpenTablesByActivity(SINCE)).thenReturn(List.of(ranking(1, "Chess", 2, 2)));

        assertNull(service.getPreferredActivity(null, SINCE).repeatPercentage());
    }

    @Test
    void noOpenTablesInThePeriodIsNotFound() {
        when(openTableRepository.findPreferredActivityTotals(SINCE)).thenReturn(totals(0, 0, 0, 0));

        assertThrows(NotFoundException.class, () -> service.getPreferredActivity(null, SINCE));
    }

    @Test
    void userWithNoOpenTablesAtAllGetsAnEmptyAnswerInsteadOfNotFound() {
        when(openTableRepository.findPreferredActivityTotals(SINCE)).thenReturn(totals(0, 0, 0, 0));

        PreferredActivityStatsDTO result = service.getPreferredActivity(7L, SINCE);

        assertNull(result.userPreferredActivity());
        assertTrue(result.ranking().isEmpty());
        assertEquals(0L, result.totalTables());
        verify(userService).getById(7L);
    }

    @Test
    void sinceIsOptionalAndMeansTheWholeHistory() {
        when(openTableRepository.findPreferredActivityTotals(any())).thenReturn(totals(0, 0, 0, 0));

        assertThrows(NotFoundException.class, () -> service.getPreferredActivity(null, null));
        verify(openTableRepository).findPreferredActivityTotals(LocalDateTime.of(1970, 1, 1, 0, 0));
    }
}
