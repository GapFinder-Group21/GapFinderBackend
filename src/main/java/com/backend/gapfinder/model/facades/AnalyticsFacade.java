package com.backend.gapfinder.model.facades;

import com.backend.gapfinder.view.dto.AppOpenDelayStatsDTO;
import com.backend.gapfinder.view.dto.InterestDemandGapBasicDTO;<<<<<<<HEAD
import com.backend.gapfinder.view.dto.PreferredActivityStatsDTO;
import com.backend.gapfinder.view.dto.OpenTableAbandonmentStatsBasicDTO;=======
import com.backend.gapfinder.view.dto.LocationUpdateRateStatsDTO;
import com.backend.gapfinder.view.dto.PreferredActivityStatsDTO;
import com.backend.gapfinder.view.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.view.dto.RejectionByScoreRangeDTO;>>>>>>>f6d346a320aa20aa3e80c9a43afba1560ab2868c
import com.backend.gapfinder.view.dto.SharedInterestGroupStatsBasicDTO;
import com.backend.gapfinder.view.dto.UnmatchedFreeTimeStatsBasicDTO;
import com.backend.gapfinder.view.dto.UnusedActivityStatsDTO;
import com.backend.gapfinder.view.dto.responses.AnalyticsDashboardResponseDTO;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.services.AnalyticsService;

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
    <<<<<<<HEAD
    // since: optional, last 30 days if null. limit: optional, all rows if null (BQ
    // 12 and BQ 13)
    // Not @Transactional on purpose: each BQ runs in its own transaction, so a "no
    // data" error in one of them
    // does not mark a shared transaction as rollback-only and break the whole
    // dashboard
    =======
    // since: optional, last 30 days if null. limit: optional, all rows if null (BQ
    // 2, BQ 12 and BQ 13)
    // Not @Transactional on purpose: each BQ runs in its own transaction, so a "no
    // data" error in one of them
    // does not mark a shared transaction as rollback-only and break the whole
    // dashboard
    >>>>>>>f6d346a320aa20aa3e80c9a43afba1560ab2868c

    public AnalyticsDashboardResponseDTO getDashboard(LocalDateTime since, Integer limit) {
        LocalDateTime start = since != null ? since : LocalDateTime.now().minusDays(DEFAULT_DAYS_BACK);

        log.info("Inicia proceso de consultar el dashboard de analíticas desde {}", start);

        AnalyticsDashboardResponseDTO dashboard = new AnalyticsDashboardResponseDTO();
<<<<<<< HEAD
=======
        dashboard.setLocationUpdatesPerHour(getLocationUpdatesPerHourOrNull(start, limit));
>>>>>>> f6d346a320aa20aa3e80c9a43afba1560ab2868c
        dashboard.setMostAbandonedStep(getMostAbandonedStepOrNull(start));
        dashboard.setAppOpenDelay(getAppOpenDelayOrNull(start));
        dashboard.setPreferredActivity(getPreferredActivityOrNull(start));
        dashboard.setGapCoverageByDuration(analyticsService.getMatchCoverageByGapDuration());
        dashboard.setConnectionMethods(analyticsService.compareConnectionMethods());
        dashboard.setBuildingsByGapPresence(analyticsService.getBuildingsByGapPresence());
        dashboard.setUnmatchedFreeTimeByCareerSemester(getUnmatchedFreeTimeOrEmpty(start, limit));
        dashboard.setInterestsByLargestFreeGroup(getInterestsByLargestFreeGroupOrEmpty(start, limit));
        dashboard.setUnusedActivities(getUnusedActivitiesOrEmpty(start));
        dashboard.setInterestDemandGap(getInterestDemandGapOrEmpty(start, limit));
<<<<<<< HEAD
=======
        dashboard.setRejectionByScoreRange(getRejectionByScoreRangeOrEmpty(start));
>>>>>>> f6d346a320aa20aa3e80c9a43afba1560ab2868c

        log.info("Termina proceso de consultar el dashboard de analíticas desde {}", start);
        return dashboard;
    }

    <<<<<<<HEAD
    // BQ 3: returns null instead of failing when there are no abandonments since
    // the given date
    =======

    // BQ 2: returns null instead of failing when there are no location updates
    // since the given date
    private LocationUpdateRateStatsDTO getLocationUpdatesPerHourOrNull(LocalDateTime since, Integer limit) {
        try {
            return analyticsService.getLocationUpdatesPerHour(since, limit);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay actualizaciones de ubicación registradas desde {}", since);
            return null;
        }
    }

    // BQ 3: returns null instead of failing when there are no abandonments since
    // the given date
    >>>>>>>f6d346a320aa20aa3e80c9a43afba1560ab2868c

    private OpenTableAbandonmentStatsBasicDTO getMostAbandonedStepOrNull(LocalDateTime since) {
        try {
            return analyticsService.getMostAbandonedStep(since);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay abandonos registrados desde {}", since);
            return null;
        }
    }

    // BQ 4: returns null instead of failing when there are no finished gaps since
    // the given date
    private AppOpenDelayStatsDTO getAppOpenDelayOrNull(LocalDateTime since) {
        try {
            return analyticsService.getAppOpenDelay(null, since);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay gaps terminados registrados desde {}", since);
            return null;
        }
    }

    // BQ 6: returns null instead of failing when there are no open tables with
    // activity since the given date
    private PreferredActivityStatsDTO getPreferredActivityOrNull(LocalDateTime since) {
        try {
            return analyticsService.getPreferredActivity(null, since);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay open tables con actividad creadas desde {}", since);
            return null;
        }
    }

    // BQ 12: returns an empty list instead of failing when there are no gaps since
    // the given date
    private List<UnmatchedFreeTimeStatsBasicDTO> getUnmatchedFreeTimeOrEmpty(LocalDateTime since, Integer limit) {
        try {
            return analyticsService.getUnmatchedFreeTimeByCareerAndSemester(since, null, limit);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay gaps registrados desde {}", since);
            return List.of();
        }
    }

    // BQ 13: returns an empty list instead of failing when there are no interests
    // with gaps since the given date
    private List<SharedInterestGroupStatsBasicDTO> getInterestsByLargestFreeGroupOrEmpty(LocalDateTime since,
            Integer limit) {
        try {
            return analyticsService.getInterestsByLargestFreeGroup(since, null, limit);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay estudiantes con intereses y gaps desde {}", since);
            return List.of();
        }
    }

    // BQ 8: returns an empty list instead of failing when there are no activities
    // No limit: the dashboard needs every activity to say how many are never chosen
    private List<UnusedActivityStatsDTO> getUnusedActivitiesOrEmpty(LocalDateTime since) {
        try {
            return analyticsService.getUnusedActivities(since, null);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay actividades registradas desde {}", since);
            return List.of();
        }
    }

    <<<<<<<HEAD=======

    // BQ 10: returns an empty list instead of failing when there are no answered
    // matches since the given date
    private List<RejectionByScoreRangeDTO> getRejectionByScoreRangeOrEmpty(LocalDateTime since) {
        try {
            return analyticsService.getRejectionByScoreRange(since);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay matches respondidos desde {}", since);
            return List.of();
        }
    }

    >>>>>>>f6d346a320aa20aa3e80c9a43afba1560ab2868c

    // BQ 14: returns an empty list instead of failing when there are no interests
    private List<InterestDemandGapBasicDTO> getInterestDemandGapOrEmpty(LocalDateTime since, Integer limit) {
        try {
            return analyticsService.getInterestsWithMostStudentsAndFewestTables(since, limit);
        } catch (NotFoundException e) {
            log.info("Dashboard: no hay intereses registrados desde {}", since);
            return List.of();
        }
    }
}