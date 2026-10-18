package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.view.dto.responses.OpenTableRecommendationResponseDTO;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.RecommendationService;
import com.backend.gapfinder.security.AccessGuard;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public OpenTableRecommendationResponseDTO recommendOpenTables(@PathVariable Long userId,
                                                                  @AuthenticationPrincipal UserModel me) {
        // Recommendations use the user's location history, so only the user can see them
        AccessGuard.requireSameUser(me, userId);
        return recommendationService.recommendOpenTables(userId);
    }
}