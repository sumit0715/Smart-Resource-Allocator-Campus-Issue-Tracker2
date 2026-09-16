package com.campusfix.concurrency;

import com.campusfix.exception.DuplicateComplaintException;
import com.campusfix.exception.InvalidComplaintException;
import com.campusfix.model.Priority;
import com.campusfix.model.User;
import com.campusfix.service.ComplaintService;
import com.campusfix.util.LoggerUtil;

import java.sql.SQLException;

/**
 * Wraps a single complaint submission so it can run on its own thread. Used to simulate
 * several students hitting "submit" at roughly the same time - ComplaintService.createComplaint
 * is synchronized, so ID generation and the history write stay consistent even under load.
 */
public class ComplaintSubmissionTask implements Runnable {

    private final ComplaintService complaintService;
    private final User student;
    private final String title;
    private final String description;
    private final String category;
    private final String location;
    private final Priority priority;

    public ComplaintSubmissionTask(ComplaintService complaintService, User student, String title,
                                    String description, String category, String location, Priority priority) {
        this.complaintService = complaintService;
        this.student = student;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.priority = priority;
    }

    @Override
    public void run() {
        try {
            var complaint = complaintService.createComplaint(title, description, category, location, priority, student);
            System.out.println(Thread.currentThread().getName() + " submitted " + complaint.getComplaintId()
                    + " for " + student.getName());
        } catch (InvalidComplaintException | DuplicateComplaintException | SQLException e) {
            LoggerUtil.error(student.getName(), "ComplaintSubmissionTask", e);
        }
    }
}
