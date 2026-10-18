package com.backend.gapfinder.enums;

public enum MatchStatusEnum {

    // Match request sent, waiting for response
    PENDING,

    // Match accepted by both students, waiting for meeting
    ACCEPTED,

    // Match rejected by the recipient
    REJECTED,

    // Match successfully completed after the meeting time
    COMPLETED
}