package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.services.AnalyticsService;
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
}