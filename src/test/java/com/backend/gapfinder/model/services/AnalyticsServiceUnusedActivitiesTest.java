package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.repositories.ActivityRepository;
import com.backend.gapfinder.model.repositories.AppSessionRepository;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.InterestRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.model.repositories.OpenTableRepository;
import com.backend.gapfinder.model.repositories.UserLocationLogRepository;
import com.backend.gapfinder.model.repositories.projections.UnusedActivityProjection;
import com.backend.gapfinder.view.dto.UnusedActivityStatsDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

// BQ 8 type 3: NEVER, RARELY and OK. RARELY is relative to an even share, so it works with 49 activities
class AnalyticsServiceUnusedActivitiesTest {

    private ActivityRepository activityRepository;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        activityRepository = mock(ActivityRepository.class);
        service = new AnalyticsService(mock(OpenTableAbandonmentService.class), mock(GapRepository.class),
                mock(UserLocationLogRepository.class), mock(MatchRepository.class), mock(OpenTableRepository.class),
                mock(AppSessionRepository.class), mock(UserService.class), activityRepository,
                mock(InterestRepository.class));
    }

    private static UnusedActivityProjection row(long id, long tables) {
        return new UnusedActivityProjection() {
            public Long getActivityId() { return id; }
            public String getActivityName() { return "Actividad " + id; }
            public String getInterestName() { return "Interés"; }
            public Long getOpenTables() { return tables; }
        };
    }

    @Test
    void anActivityWithoutTablesIsNever() {
        assertEquals("NEVER", AnalyticsService.classifyActivity(0, 0.0, 49));
    }

    @Test
    void withFortyNineActivitiesAnEvenSpreadIsNotRarely() {
        // An even share is 100 / 49 = 2.04 %: with the old fixed 5 % every activity would have been RARELY
        assertEquals("OK", AnalyticsService.classifyActivity(10, 100.0 / 49, 49));
    }

    @Test
    void anActivityUnderHalfOfAnEvenShareIsRarely() {
        // Half of an even share with 49 activities is 1.02 %
        assertEquals("RARELY", AnalyticsService.classifyActivity(1, 1.0, 49));
        assertEquals("OK", AnalyticsService.classifyActivity(2, 1.03, 49));
    }

    @Test
    void theCutDependsOnHowManyActivitiesThereAre() {
        // With 4 activities an even share is 25 %: under 12.5 % is RARELY
        assertEquals("RARELY", AnalyticsService.classifyActivity(1, 10.0, 4));
        assertEquals("OK", AnalyticsService.classifyActivity(2, 13.0, 4));
    }

    @Test
    void theServiceClassifiesEveryRowWithTheNumberOfActivities() {
        // 4 activities, 40 open tables: 0, 2 (5 %), 10 (25 %), 28 (70 %)
        when(activityRepository.findActivityUsageSince(any())).thenReturn(
                List.of(row(1, 0), row(2, 2), row(3, 10), row(4, 28)));

        List<UnusedActivityStatsDTO> stats = service.getUnusedActivities(null, null);

        List<String> classes = new ArrayList<>();
        stats.forEach(s -> classes.add(s.classification()));
        assertEquals(List.of("NEVER", "RARELY", "OK", "OK"), classes);
        assertEquals(5.0, stats.get(1).percentage(), 0.0001);
    }
}
