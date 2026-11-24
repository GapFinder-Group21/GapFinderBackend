package com.backend.gapfinder.model.exceptions;

// Thrown when the requested action was already done (for example, the user already joined the table).
// It is the only case that answers 409: the app treats it as "done" and does not retry.
public class AlreadyDoneException extends RuntimeException {
    public AlreadyDoneException(String message) {
        super(message);
    }
}
