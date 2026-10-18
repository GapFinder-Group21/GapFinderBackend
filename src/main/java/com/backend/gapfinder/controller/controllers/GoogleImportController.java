package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.responses.GoogleImportResult;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.services.GoogleScheduleImportService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/google")
public class GoogleImportController {

    private final GoogleScheduleImportService importService;

    public GoogleImportController(GoogleScheduleImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import-schedule")
    public GoogleImportResult importSchedule(@AuthenticationPrincipal UserModel user) throws Exception {
        return importService.importSchedule(user.getId());
    }
}
