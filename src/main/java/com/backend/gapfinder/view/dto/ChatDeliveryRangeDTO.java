package com.backend.gapfinder.view.dto;

// DTO with how many chat messages of each route took a range of time to reach the server (BQ 17)
public record ChatDeliveryRangeDTO(

    // '<1s', '1-3s', '3-10s', '10-60s', '1-10min' or '10min+' (a message of exactly 3 s is in '3-10s')
    String range,

    // Messages sent at once in the range
    long direct,

    // Messages sent by the offline queue in the range
    long queued
) {}
