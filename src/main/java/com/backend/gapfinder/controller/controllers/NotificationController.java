package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.view.dto.NotificationBasicDTO;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.NotificationService;
import com.backend.gapfinder.security.AccessGuard;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    public NotificationController(NotificationService notificationService, ModelMapper modelMapper) {
        this.notificationService = notificationService;
        this.modelMapper = modelMapper;
    }

    // All notifications of a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationBasicDTO>> getByUser(@PathVariable Long userId,
                                                                @AuthenticationPrincipal UserModel me) {
        // A user can only see and change their own notifications
        AccessGuard.requireSameUser(me, userId);
        List<NotificationBasicDTO> result = notificationService.getByUser(userId).stream()
                .map(n -> modelMapper.map(n, NotificationBasicDTO.class))
                .toList();
        return ResponseEntity.ok(result);
    }

    // Unread notifications of a user (endpoint used by the front polling)
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationBasicDTO>> getUnreadByUser(@PathVariable Long userId,
                                                                      @AuthenticationPrincipal UserModel me) {
        // A user can only see and change their own notifications
        AccessGuard.requireSameUser(me, userId);
        List<NotificationBasicDTO> result = notificationService.getUnreadByUser(userId).stream()
                .map(n -> modelMapper.map(n, NotificationBasicDTO.class))
                .toList();
        return ResponseEntity.ok(result);
    }

    // Mark one notification as read
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationBasicDTO> markAsRead(@PathVariable Long notificationId,
                                                           @RequestParam Long userId,
                                                           @AuthenticationPrincipal UserModel me) {
        // A user can only see and change their own notifications
        AccessGuard.requireSameUser(me, userId);
        NotificationBasicDTO result = modelMapper.map(
                notificationService.markAsRead(notificationId, userId), NotificationBasicDTO.class);
        return ResponseEntity.ok(result);
    }

    // Mark all notifications of a user as read
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId, @AuthenticationPrincipal UserModel me) {
        // A user can only see and change their own notifications
        AccessGuard.requireSameUser(me, userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
