package com.backend.gapfinder.enums;

public enum NotificationTypeEnum {

    // Someone sent the user a friend request
    FRIEND_REQUEST_SENT,

    // The receiver accepted the user's friend request
    FRIEND_REQUEST_ACCEPTED,

    // The receiver rejected the user's friend request
    FRIEND_REQUEST_REJECTED,

    // Someone proposed a match to the user
    MATCH_PROPOSED,

    // The user's match proposal was accepted
    MATCH_ACCEPTED,

    // The user's match proposal was rejected
    MATCH_REJECTED,

    // Someone joined the user's open table
    OPEN_TABLE_JOINED
}
