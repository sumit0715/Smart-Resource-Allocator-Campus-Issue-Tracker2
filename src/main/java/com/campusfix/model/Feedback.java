package com.campusfix.model;

import java.time.LocalDateTime;

public class Feedback {

    private int feedbackId;
    private final String complaintId;
    private final int userId;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;

    public Feedback(String complaintId, int userId, int rating, String comment) {
        this.complaintId = complaintId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public void setFeedbackId(int feedbackId) {
        this.feedbackId = feedbackId;
    }

    public int getFeedbackId() {
        return feedbackId;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public int getUserId() {
        return userId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
