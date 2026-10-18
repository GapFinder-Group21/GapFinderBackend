package com.backend.gapfinder.view.dto;

// DTO with the minutes after the start of a gap at which the reminder should be sent
public record AppOpenDelayDTO(

    // Minutes after the gap start to send the reminder (whole minutes, between 2 and 30)
    int offsetMinutes,

    // Where the offset comes from: USER, GLOBAL or DEFAULT
    String source,

    // Average minutes the user takes to open the app after a gap starts (null if no data)
    Double userAverageMinutes,

    // Number of gaps of the user used for the average
    long userGapsCounted,

    // Average minutes all users take to open the app after a gap starts (null if no data)
    Double globalAverageMinutes,

    // Number of gaps of all users used for the average
    long globalGapsCounted
) {}
