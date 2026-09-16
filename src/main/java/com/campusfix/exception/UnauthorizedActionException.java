package com.campusfix.exception;

/** Thrown when a user tries to do something their role doesn't allow. */
public class UnauthorizedActionException extends Exception {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
