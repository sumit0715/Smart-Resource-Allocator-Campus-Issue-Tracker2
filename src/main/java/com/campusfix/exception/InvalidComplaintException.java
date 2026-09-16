package com.campusfix.exception;

/** Thrown when complaint input fails validation (empty title, unknown category, etc). */
public class InvalidComplaintException extends Exception {
    public InvalidComplaintException(String message) {
        super(message);
    }
}
