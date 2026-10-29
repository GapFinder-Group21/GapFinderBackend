package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.view.dto.AppOpenDelayStatsDTO;
import com.backend.gapfinder.view.dto.LocationUpdateRateStatsDTO;
import com.backend.gapfinder.view.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.view.dto.PreferredActivityStatsDTO;
import com.backend.gapfinder.view.dto.RejectionByScoreRangeDTO;
import com.backend.gapfinder.view.dto.SharedInterestGroupStatsBasicDTO;
import com.backend.gapfinder.view.dto.UnmatchedFreeTimeStatsBasicDTO;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.view.dto.UnusedActivityStatsDTO;
import com.backend.gapfinder.model.services.AnalyticsService;
import com.backend.gapfinder.security.AccessGuard;
import com.backend.gapfinder.view.dto.responses.ConnectionMethodResponseDTO;
import com.backend.gapfinder.view.dto.responses.BuildingGapPresenceResponseDTO;
import com.backend.gapfinder.view.dto.responses.GapCoverageResponseDTO;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // ==================== BQ 2 GRUPAL TYPE 1 ====================

    // How many location updates per hour does the backend receive from each student while they are inside a campus building?
    // Get the updates per student-hour (average, median, max and distribution) and the rate of each student
    // GET /analytics/location-updates/per-hour?since=2026-09-01T00:00:00&limit=10
    @GetMapping("/location-updates/per-hour")
    public LocationUpdateRateStatsDTO getLocationUpdatesPerHour(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) Integer limit) {

        return analyticsService.getLocationUpdatesPerHour(since, limit);
    }

    // ================== END BQ 2 GRUPAL TYPE 1 ==================

    // BQ 3: At which step do students most often leave the Open Table creating process?
    // Get the creation step with the highest abandonment rate since the given date
    // GET /analytics/open-tables/most-abandoned-step?since=2026-09-01T00:00:00
    @GetMapping("/open-tables/most-abandoned-step")
    public OpenTableAbandonmentStatsBasicDTO getMostAbandonedStep(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        return analyticsService.getMostAbandonedStep(since);
    }

    
    // ==================== BQ 5 ====================

    // Get match coverage metrics grouped by gap duration, sorted by lowest coverage
    // GET /analytics/gap-coverage/by-duration
    @GetMapping("/gap-coverage/by-duration")
    public List<GapCoverageResponseDTO> getMatchCoverageByGapDuration() {
        return analyticsService.getMatchCoverageByGapDuration();
    }

    // ================== END BQ 5 ==================

    // ==================== BQ 7 ====================

    // Compare the completion percentage of matches and open tables to see which leads to more meetups
    // GET /analytics/connection-methods/accepted-meetups
    @GetMapping("/connection-methods/accepted-meetups")
    public ConnectionMethodResponseDTO compareConnectionMethods() {
        return analyticsService.compareConnectionMethods();
    }

    // ================== END BQ 7 ==================
    // ==================== BQ 12 INDIVIDUAL ====================

    // Which careers and semesters have the highest rate of unmatched free time on campus?
    // Get the (career, semester) groups ranked by unmatched free time rate, highest first
    // GET /analytics/free-time/unmatched-by-career-semester?from=2026-09-01T00:00:00&to=2026-10-01T00:00:00&limit=10
    @GetMapping("/free-time/unmatched-by-career-semester")
    public List<UnmatchedFreeTimeStatsBasicDTO> getUnmatchedFreeTimeByCareerAndSemester(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer limit) {

        return analyticsService.getUnmatchedFreeTimeByCareerAndSemester(from, to, limit);
    }

    // ================== END BQ 12 INDIVIDUAL ==================

        // ==================== BQ 11 ====================

    // Get student presence and free time metrics aggregated by campus building
    // GET /analytics/buildings/gap-presence
    @GetMapping("/buildings/gap-presence")
    public List<BuildingGapPresenceResponseDTO> getBuildingsByGapPresence() {
        return analyticsService.getBuildingsByGapPresence();
    }

    // ================== END BQ 11 ==================

    // ==================== BQ 13 INDIVIDUAL ====================

    // Which interests are shared by the largest groups of students who are free at the same time?
    // Get the interests ranked by the largest group of students sharing them who are free at the same time
    // GET /analytics/interests/largest-free-groups?from=2026-09-01T00:00:00&to=2026-10-01T00:00:00&limit=10
    @GetMapping("/interests/largest-free-groups")
    public List<SharedInterestGroupStatsBasicDTO> getInterestsByLargestFreeGroup(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer limit) {

        return analyticsService.getInterestsByLargestFreeGroup(from, to, limit);
    }

    // ================== END BQ 13 INDIVIDUAL ==================

    // ==================== BQ 4 INDIVIDUAL TYPE 2 ====================

    // How long after a gap starts do students open the app?
    // Get the distribution of the minutes after a gap starts at which the app is first opened
    // GET /analytics/app-open-delay?since=2026-09-01T00:00:00
    @GetMapping("/app-open-delay")
    public AppOpenDelayStatsDTO getAppOpenDelay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        return analyticsService.getAppOpenDelay(null, since);
    }

    // Same distribution plus the minutes after a gap starts at which the app should remind the user
    // GET /analytics/app-open-delay/user/{userId}?since=2026-09-01T00:00:00
    @GetMapping("/app-open-delay/user/{userId}")
    public AppOpenDelayStatsDTO getAppOpenDelayForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        return analyticsService.getAppOpenDelay(userId, since);
    }

    // ================== END BQ 4 INDIVIDUAL TYPE 2 ==================

    // ==================== BQ 6 INDIVIDUAL TYPE 2 ====================

    // Which activity does the student choose most often when creating open tables?
    // Get the activities most preferred by all the students and the share of students who always repeat the same one
    // GET /analytics/preferred-activity?since=2026-09-01T00:00:00
    @GetMapping("/preferred-activity")
    public PreferredActivityStatsDTO getPreferredActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        return analyticsService.getPreferredActivity(null, since);
    }

    // Same answer plus the activity the user chooses most often (null if they created no open tables)
    // The app preselects it when the user creates an open table
    // GET /analytics/preferred-activity/user/{userId}?since=2026-09-01T00:00:00
    @GetMapping("/preferred-activity/user/{userId}")
    public PreferredActivityStatsDTO getPreferredActivityForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @AuthenticationPrincipal UserModel me) {

        // The preferred activity of a user is personal: only the user can ask for it
        AccessGuard.requireSameUser(me, userId);
        return analyticsService.getPreferredActivity(userId, since);
    }

    // ================== END BQ 6 INDIVIDUAL TYPE 2 ==================
    // ==================== BQ 8 INDIVIDUAL TYPE 3 ====================

    // Which activities are never or rarely chosen when creating open tables?
    // Get all the activities from the least to the most chosen, classified as NEVER, RARELY or OK
    // GET /analytics/activities/unused?since=2026-09-01T00:00:00&limit=10
    @GetMapping("/activities/unused")
    public List<UnusedActivityStatsDTO> getUnusedActivities(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) Integer limit) {

        return analyticsService.getUnusedActivities(since, limit);
    }

    // ================== END BQ 8 INDIVIDUAL TYPE 3 ==================

    // ==================== BQ 10 INDIVIDUAL TYPE 3 ====================

    // In which match score range are match proposals most often rejected?
    // Get the 5 score ranges in natural order with their rejection rate, marking the highest one
    // GET /analytics/matches/rejection-by-score?since=2026-09-01T00:00:00
    @GetMapping("/matches/rejection-by-score")
    public List<RejectionByScoreRangeDTO> getRejectionByScoreRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        return analyticsService.getRejectionByScoreRange(since);
    }

    // ================== END BQ 10 INDIVIDUAL TYPE 3 ==================
}
