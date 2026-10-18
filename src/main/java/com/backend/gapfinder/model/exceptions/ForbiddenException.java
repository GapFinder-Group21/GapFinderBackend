package com.backend.gapfinder.model.exceptions;

// Thrown when a user tries to act on data that belongs to another user
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
