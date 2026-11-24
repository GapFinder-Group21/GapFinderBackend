package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.ChatDeliveryReportService;
import com.backend.gapfinder.security.AccessGuard;
import com.backend.gapfinder.view.dto.requests.ChatDeliveryReportRequest;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// BQ 17 INDIVIDUAL TYPE 1: the app reports how long each chat message took to reach the server
@RestController
@RequestMapping("/chat-delivery-reports")
public class ChatDeliveryReportController {

    private final ChatDeliveryReportService reportService;

    public ChatDeliveryReportController(ChatDeliveryReportService reportService) {
        this.reportService = reportService;
    }

    // Saves the delivery time of one message sent by the user
    // POST /chat-delivery-reports/user/{userId}
    // body {"clientId": "…", "route": "QUEUED", "elapsedMs": 94200, "afterReconnectMs": 3100, "tappedAt": "2026-11-24T10:02:00"}
    @PostMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerReport(@PathVariable Long userId,
                               @RequestBody ChatDeliveryReportRequest request,
                               @AuthenticationPrincipal UserModel me) {

        // A user can only report their own messages
        AccessGuard.requireSameUser(me, userId);

        reportService.register(userId, request);
    }
}
