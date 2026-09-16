package com.campusfix.model;

/** Anything that has a status a user can follow the progress of. */
public interface Trackable {
    void updateStatus(ComplaintStatus newStatus);
    ComplaintStatus getStatus();
}
