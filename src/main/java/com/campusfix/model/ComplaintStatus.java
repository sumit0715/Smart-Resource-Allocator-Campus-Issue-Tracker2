package com.campusfix.model;

/**
 * Lifecycle states a complaint can be in.
 * Normal path: SUBMITTED -> UNDER_REVIEW -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
 * Side paths: SUBMITTED/UNDER_REVIEW -> REJECTED, IN_PROGRESS <-> ON_HOLD
 */
public enum ComplaintStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ASSIGNED,
    IN_PROGRESS,
    ON_HOLD,
    RESOLVED,
    CLOSED,
    REJECTED
}
