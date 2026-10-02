package com.backend.gapfinder.facades;

import com.backend.gapfinder.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.dto.SharedInterestGroupStatsBasicDTO;
import com.backend.gapfinder.dto.UnmatchedFreeTimeStatsBasicDTO;
import com.backend.gapfinder.dto.responses.AnalyticsDashboardResponseDTO;
import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.services.AnalyticsService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// Facade of the Facade pattern: gives the dashboard a single entry point to all the business questions,
// hiding which method answers each one, their parameters and the "no data" errors some of them throw
@Slf4j
@Service
public class AnalyticsFacade {

    // Days used as the default start date when the dashboard does not send one
    private static final int DEFAULT_DAYS_BACK = 30;

    private final AnalyticsService analyticsService;

    // Dependency injection constructor
    public AnalyticsFacade(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // Collects the answers of all the business questions in a single response
    // since: optional, last 30 days if null. limit: optional, all rows if null (BQ 12 and BQ 13)
    // Not @Transactional on purpose: each BQ runs in its own transaction, so a "no data" error in one of them
    // does not mark a shared transaction as rollback-only and break the whole dashboard
    public AnalyticsDashboardResponseDTO getDashboard(LocalDateTime since, Integer limit) {
        LocalDateTime start = since != null ? since : LocalDateTime.now().minusDays(DEFAULT_DAYS_BACK);

        log.info("Inicia proceso de consultar el dashboard de analíticas desde {}", start);

        AnalyticsDashboardResponseDTO dashboard = new AnalyticsDashboardResponseDTO();
        dashboard.setMostAbandonedStep(getMostAbandonedStepOrNull(start));
        dashboard.setGapCoverageByDuration(analyticsService.getMatchCoverageByGapDuration());
        dashboard.setConnectionMethods(analyticsService.compareConnectionMethods());
        dashboard.setBuildingsByGapPresence(analyticsService.getBuildingsByGapPresence());
        dashboard.setUnmatchedFreeTimeByCareerSemester(getUnmatchedFreeTimeOrEmpty(start, limit));
        dashboard.setInterestsByLargestFreeGroup(getInterestsByLargestFreeGroupOrEmpty(start, limit));

        log.info("Termina proceso de consultar el dashboard de analíticas desde {}", start);
        return dashboard;
    }

    // BQ 3: returns null instead of failing when there are no abandonments since the given date
    private OpenTableAbandonmentStatsBasicDTO getMostAbandonedStepOrNull(LocalDateTime since) {
        try {
            return analyticsService.getMostAbandonedStep(since);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay abandonos registrados desde {}", since);
            return null;
        }
    }

    // BQ 12: returns an empty list instead of failing when there are no gaps since the given date
    private List<UnmatchedFreeTimeStatsBasicDTO> getUnmatchedFreeTimeOrEmpty(LocalDateTime since, Integer limit) {
        try {
            return analyticsService.getUnmatchedFreeTimeByCareerAndSemester(since, null, limit);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay gaps registrados desde {}", since);
            return List.of();
        }
    }

    // BQ 13: returns an empty list instead of failing when there are no interests with gaps since the given date
    private List<SharedInterestGroupStatsBasicDTO> getInterestsByLargestFreeGroupOrEmpty(LocalDateTime since,
                                                                                         Integer limit) {
        try {
            return analyticsService.getInterestsByLargestFreeGroup(since, null, limit);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay estudiantes con intereses y gaps desde {}", since);
            return List.of();
        }
    }
}