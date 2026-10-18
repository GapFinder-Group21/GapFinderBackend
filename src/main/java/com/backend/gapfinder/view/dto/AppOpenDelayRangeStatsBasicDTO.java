package com.backend.gapfinder.view.dto;

// DTO with the gaps whose first app opening falls in a range of minutes after the gap start
public record AppOpenDelayRangeStatsBasicDTO(

    // Range of minutes: '0-5', '5-15', '15-30', '30+' or 'No abrió' (the app was not opened during the gap)
    String range,

    // Number of gaps in the range
    long gaps,

    // Percentage of the gaps of the period that fall in the range, between 0 and 100
    double percentage
) {}
