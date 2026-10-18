package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.view.dto.responses.OpenTableRecommendationResponseDTO;
import com.backend.gapfinder.model.services.RecommendationService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    // Get open tables recommended for a user, based on their favorite building and active gaps
    // GET /recommendations/user/{userId}/open-tables
    @GetMapping("/user/{userId}/open-tables")
    public OpenTableRecommendationResponseDTO recommendOpenTables(@PathVariable Long userId) {
        return recommendationService.recommendOpenTables(userId);
    }
}