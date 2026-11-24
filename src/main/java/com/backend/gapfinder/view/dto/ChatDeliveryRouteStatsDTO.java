package com.backend.gapfinder.view.dto;

// DTO with the delivery times of the chat messages of one route (BQ 17)
public record ChatDeliveryRouteStatsDTO(

    // "DIRECT" (sent at once) or "QUEUED" (sent later by the offline queue)
    String route,

    // Reported messages of the route
    long messages,

    // Median milliseconds from the tap on Send until the server saved the message (null if the route has no messages)
    Double medianMs,

    // 90th percentile of the same time: 9 of every 10 messages took this long or less (null if no messages)
    Double p90Ms,

    // Only QUEUED: median milliseconds from the moment the connection came back until the server saved
    // the message (null for DIRECT or when the app did not know it)
    Double medianAfterReconnectMs
) {}
