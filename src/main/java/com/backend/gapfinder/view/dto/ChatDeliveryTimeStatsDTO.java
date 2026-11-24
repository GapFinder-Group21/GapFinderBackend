package com.backend.gapfinder.view.dto;

import java.util.List;

// DTO with the answer of the BQ 17: how long chat messages take from the tap on Send until the server saves them,
// when they go out at once and when they wait in the offline queue
public record ChatDeliveryTimeStatsDTO(

    // Reported messages in the period (both routes)
    long messages,

    // Share of the messages that waited in the offline queue, from 0 to 100
    double queuedPercent,

    // Share of the messages sent at once that took 3 s or more, from 0 to 100 (0 if there are none)
    double slowDirectPercent,

    // Times of the messages sent at once
    ChatDeliveryRouteStatsDTO direct,

    // Times of the messages sent by the offline queue
    ChatDeliveryRouteStatsDTO queued,

    // Messages per range of time, the 6 ranges in natural order (the empty ones too)
    List<ChatDeliveryRangeDTO> distribution
) {}
