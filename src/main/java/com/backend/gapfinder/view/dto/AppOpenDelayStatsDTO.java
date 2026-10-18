package com.backend.gapfinder.view.dto;

import java.util.List;

// DTO with how long after a gap starts the students open the app, and the reminder offset of a user
public record AppOpenDelayStatsDTO(

    // Gaps per range of minutes, in natural order, including the ranges without gaps
    List<AppOpenDelayRangeStatsBasicDTO> distribution,

    // Range in which the app is most often opened (null if it was not opened in any gap)
    String mostFrequentRange,

    // Average minutes all users take to open the app after a gap starts (null if no data)
    Double globalAverageMinutes,

    // Number of gaps of all users used for the average
    long globalGapsCounted,

    // Average minutes the user takes to open the app (null if no user was given or they have no data)
    Double userAverageMinutes,

    // Number of gaps of the user used for the average (null if no user was given)
    Long userGapsCounted,

    // Minutes after the gap start to send the reminder, whole minutes between 2 and 30 (null if no user was given)
    Integer offsetMinutes,

    // Where the offset comes from: USER, GLOBAL or DEFAULT (null if no user was given)
    String source
) {}
