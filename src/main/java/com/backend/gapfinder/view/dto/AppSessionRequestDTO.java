package com.backend.gapfinder.view.dto;

import lombok.Data;

import java.time.LocalDateTime;

// Optional body of POST /app-sessions/user/{userId}
@Data
public class AppSessionRequestDTO {

    // When the app was really opened on the phone (local date time). An opening queued without connection
    // is sent later, so the phone tells when it happened. Without it the server uses the current time
    private LocalDateTime openedAt;
}
