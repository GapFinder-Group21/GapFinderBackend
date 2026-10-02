package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.dto.SharedInterestGroupStatsBasicDTO;
import com.backend.gapfinder.dto.UnmatchedFreeTimeStatsBasicDTO;
import com.backend.gapfinder.services.AnalyticsService;
import com.backend.gapfinder.dto.responses.ConnectionMethodResponseDTO;
import com.backend.gapfinder.dto.responses.BuildingGapPresenceResponseDTO;
import com.backend.gapfinder.dto.responses.GapCoverageResponseDTO;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

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
}