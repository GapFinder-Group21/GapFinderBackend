package com.backend.gapfinder.view.dto.requests;

import java.time.LocalDateTime;

import lombok.Data;

// Body of POST /chat-delivery-reports/user/{userId} (BQ 17 INDIVIDUAL TYPE 1)
@Data
public class ChatDeliveryReportRequest {

    // Id the app generated for the message (UUID), the same one it sent to POST /chats/messages
    private String clientId;

    // "DIRECT" (sent at once) or "QUEUED" (sent later by the offline queue). It is a String and not the enum
    // so a wrong value is a clear 400 and not a JSON error
    private String route;

    // Milliseconds from the tap on Send until the server saved the message
    private Long elapsedMs;

    // Only for QUEUED: milliseconds from the moment the connection came back until the server saved the message
    private Long afterReconnectMs;

    // When the student tapped Send (local date time of the phone)
    private LocalDateTime tappedAt;
}
