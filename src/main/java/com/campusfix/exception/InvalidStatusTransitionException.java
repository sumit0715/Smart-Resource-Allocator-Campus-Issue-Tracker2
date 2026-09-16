package com.campusfix.exception;

import com.campusfix.model.ComplaintStatus;

/** Thrown when a requested status change doesn't follow the allowed workflow. */
public class InvalidStatusTransitionException extends Exception {
    public InvalidStatusTransitionException(ComplaintStatus from, ComplaintStatus to) {
        super("Cannot move a complaint from " + from + " to " + to);
    }
}
