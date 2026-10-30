package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.AppSessionService;
import com.backend.gapfinder.security.AccessGuard;
import com.backend.gapfinder.view.dto.AppSessionRequestDTO;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app-sessions")
public class AppSessionController {

    private final AppSessionService appSessionService;

    public AppSessionController(AppSessionService appSessionService) {
        this.appSessionService = appSessionService;
    }

    // Register that a user opened the app. The body is optional: {"openedAt": "2026-10-30T10:02:00"} is the time
    // the phone saw (an opening queued offline arrives later); without it the server uses the current time
    // POST /app-sessions/user/{userId}
    @PostMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAppSession(@PathVariable Long userId,
                                   @RequestBody(required = false) AppSessionRequestDTO body,
                                   @AuthenticationPrincipal UserModel me) {

        // A user can only register their own app openings
        AccessGuard.requireSameUser(me, userId);

        appSessionService.register(userId, body != null ? body.getOpenedAt() : null);
    }
}
