package com.backend.gapfinder.model.enums;

// How a chat message reached the server (BQ 17 INDIVIDUAL TYPE 1)
public enum ChatDeliveryRouteEnum {

    // The app sent it right after the student tapped Send, and the first try worked
    DIRECT,

    // It waited in the app's offline queue (there was no connection, or the first try failed)
    // and the queue sent it later
    QUEUED
}
