package com.campusfix.service;

import com.campusfix.dao.ComplaintDAO;
import com.campusfix.dao.FeedbackDAO;
import com.campusfix.exception.ComplaintNotFoundException;
import com.campusfix.exception.InvalidComplaintException;
import com.campusfix.exception.UnauthorizedActionException;
import com.campusfix.model.Complaint;
import com.campusfix.model.ComplaintStatus;
import com.campusfix.model.Feedback;
import com.campusfix.model.User;
import com.campusfix.util.LoggerUtil;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Feedback is only accepted once a complaint has actually been dealt with - the business
 * rules call that out explicitly, so it's enforced here rather than left to the UI.
 */
public class FeedbackService {

    private static final Set<ComplaintStatus> FEEDBACK_ELIGIBLE = EnumSet.of(ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED);

    private final FeedbackDAO feedbackDAO;
    private final ComplaintDAO complaintDAO;

    public FeedbackService(FeedbackDAO feedbackDAO, ComplaintDAO complaintDAO) {
        this.feedbackDAO = feedbackDAO;
        this.complaintDAO = complaintDAO;
    }

    public Feedback submitFeedback(String complaintId, User student, int rating, String comment)
            throws SQLException, ComplaintNotFoundException, InvalidComplaintException, UnauthorizedActionException {
        Complaint complaint = complaintDAO.findById(complaintId);

        if (complaint.getSubmittedBy() != student.getUserId()) {
            throw new UnauthorizedActionException("You can only give feedback on your own complaints.");
        }
        if (!FEEDBACK_ELIGIBLE.contains(complaint.getStatus())) {
            throw new InvalidComplaintException("Feedback can only be submitted once a complaint is resolved.");
        }
        if (rating < 1 || rating > 5) {
            throw new InvalidComplaintException("Rating must be between 1 and 5.");
        }

        Feedback feedback = new Feedback(complaintId, student.getUserId(), rating, comment);
        feedbackDAO.save(feedback);
        LoggerUtil.log(student.getName(), "FEEDBACK_SUBMITTED", complaintId + " rated " + rating + "/5");
        return feedback;
    }

    public List<Feedback> getFeedbackFor(String complaintId) throws SQLException {
        return feedbackDAO.findByComplaintId(complaintId);
    }

    public double getAverageRating() throws SQLException {
        return feedbackDAO.averageRating();
    }
}
