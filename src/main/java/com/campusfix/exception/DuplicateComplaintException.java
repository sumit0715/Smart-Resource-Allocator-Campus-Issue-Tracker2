package com.campusfix.exception;

/** Thrown when the same complaint looks like it has already been submitted very recently. */
public class DuplicateComplaintException extends Exception {
    public DuplicateComplaintException(String message) {
        super(message);
    }
}
