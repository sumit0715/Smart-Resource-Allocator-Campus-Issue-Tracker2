package com.campusfix.exception;

/** Thrown when a complaint ID doesn't match any record. */
public class ComplaintNotFoundException extends Exception {
    public ComplaintNotFoundException(String complaintId) {
        super("No complaint found with ID: " + complaintId);
    }
}
