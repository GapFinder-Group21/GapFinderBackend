package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.StudyModeService;
import com.backend.gapfinder.security.AccessGuard;
import com.backend.gapfinder.view.dto.StudyPeriodDTO;
import com.backend.gapfinder.view.dto.requests.StudyPeriodRequest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/study-mode")
public class StudyModeController {

    private final StudyModeService studyModeService;

    public StudyModeController(StudyModeService studyModeService) {
        this.studyModeService = studyModeService;
    }

    // Study periods that are active now or start later, the earliest first
    // GET /users/{userId}/study-mode
    @GetMapping
    public List<StudyPeriodDTO> getActivePeriods(@PathVariable Long userId,
                                                 @AuthenticationPrincipal UserModel me) {
        // A user can only read their own study periods
        AccessGuard.requireSameUser(me, userId);
        return studyModeService.getActivePeriods(userId).stream()
                .map(StudyPeriodDTO::from)
                .toList();
    }

    // Saves a study period: from now on the user is hidden from the matches and from the nearby friends
    // POST /users/{userId}/study-mode body {startTime, endTime, gapId?, clientId}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyPeriodDTO create(@PathVariable Long userId,
                                 @RequestBody StudyPeriodRequest request,
                                 @AuthenticationPrincipal UserModel me) {
        AccessGuard.requireSameUser(me, userId);
        return StudyPeriodDTO.from(studyModeService.create(
                userId, request.getStartTime(), request.getEndTime(), request.getGapId(), request.getClientId()));
    }

    // Removes a study period (204 also when it does not exist)
    // DELETE /users/{userId}/study-mode/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId,
                       @PathVariable Long id,
                       @AuthenticationPrincipal UserModel me) {
        AccessGuard.requireSameUser(me, userId);
        studyModeService.delete(userId, id);
    }
}
