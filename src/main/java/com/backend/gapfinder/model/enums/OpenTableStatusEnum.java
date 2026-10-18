package com.backend.gapfinder.model.enums;

public enum OpenTableStatusEnum {

    // Table is open and accepting new participants
    OPEN,

    // Table has reached its maximum participant limit
    FULL,

    // Table session was successfully completed
    COMPLETED,

    // Table expired without any participants joining
    EMPTY
}