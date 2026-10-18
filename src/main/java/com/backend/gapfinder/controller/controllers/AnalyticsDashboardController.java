package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.responses.AnalyticsDashboardResponseDTO;
import com.backend.gapfinder.facades.AnalyticsFacade;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
public class AnalyticsDashboardController {

    private final AnalyticsFacade analyticsFacade;

    public AnalyticsDashboardController(AnalyticsFacade analyticsFacade) {
        this.analyticsFacade = analyticsFacade;
    }

    // Get the answers of all the business questions in a single call for the analytics dashboard
    // GET /analytics/dashboard?since=2026-09-01T00:00:00&limit=5
    @GetMapping("/dashboard")
    public AnalyticsDashboardResponseDTO getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) Integer limit) {

        return analyticsFacade.getDashboard(since, limit);
    }
}