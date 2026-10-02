package com.backend.gapfinder.exceptions;

// Thrown when an external schedule source (e.g. Google Calendar) fails; keeps the original error as its cause
public class ScheduleSourceUnavailableException extends RuntimeException {

    public ScheduleSourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
