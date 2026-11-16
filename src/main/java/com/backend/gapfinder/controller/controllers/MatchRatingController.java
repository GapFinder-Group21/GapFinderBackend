package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.MatchRatingService;
import com.backend.gapfinder.security.AccessGuard;
import com.backend.gapfinder.view.dto.MatchHistoryItemDTO;
import com.backend.gapfinder.view.dto.requests.MatchRatingRequest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// "Mis matches": history of matches and their ratings
@RestController
@RequestMapping("/matches")
public class MatchRatingController {

    private final MatchRatingService ratingService;

    public MatchRatingController(MatchRatingService ratingService) {
        this.ratingService = ratingService;
    }

    // All the matches of the user, newest first, with my rating and the other one (only after I rate)
    // GET /matches/user/{userId}/history
    @GetMapping("/user/{userId}/history")
    public List<MatchHistoryItemDTO> getHistory(@PathVariable Long userId,
                                                @AuthenticationPrincipal UserModel me) {
        AccessGuard.requireSameUser(me, userId);
        return ratingService.getHistory(userId);
    }

    // Rate a finished match; the rater is always the authenticated user
    // POST /matches/{matchId}/ratings body {stars, comment}
    @PostMapping("/{matchId}/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchHistoryItemDTO rate(@PathVariable Long matchId,
                                    @RequestBody MatchRatingRequest request,
                                    @AuthenticationPrincipal UserModel me) {
        if (me == null) {
            throw new ForbiddenException("No tienes permiso para calificar este match");
        }
        return ratingService.rate(matchId, me.getId(), request.getStars(), request.getComment());
    }
}
