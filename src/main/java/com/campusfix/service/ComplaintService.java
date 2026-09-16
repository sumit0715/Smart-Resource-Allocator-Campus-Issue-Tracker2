package com.campusfix.service;

import com.campusfix.dao.ComplaintDAO;
import com.campusfix.exception.ComplaintNotFoundException;
import com.campusfix.exception.DuplicateComplaintException;
import com.campusfix.exception.InvalidComplaintException;
import com.campusfix.exception.InvalidStatusTransitionException;
import com.campusfix.exception.UnauthorizedActionException;
import com.campusfix.model.Complaint;
import com.campusfix.model.ComplaintHistory;
import com.campusfix.model.ComplaintStatus;
import com.campusfix.model.Priority;
import com.campusfix.model.Role;
import com.campusfix.model.User;
import com.campusfix.util.ComplaintIdGenerator;
import com.campusfix.util.FileManager;
import com.campusfix.util.InputValidator;
import com.campusfix.util.LoggerUtil;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Owns the complaint lifecycle: creation, status transitions, resolution, closure and cancellation.
 * Every state change is written to COMPLAINT_HISTORY so nothing about a complaint's past gets lost.
 */
public class ComplaintService {

    // legal next states for each status - anything not listed here is rejected
    private static final Map<ComplaintStatus, Set<ComplaintStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ComplaintStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(ComplaintStatus.SUBMITTED, EnumSet.of(ComplaintStatus.UNDER_REVIEW, ComplaintStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.UNDER_REVIEW, EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.ASSIGNED, EnumSet.of(ComplaintStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.IN_PROGRESS, EnumSet.of(ComplaintStatus.ON_HOLD, ComplaintStatus.RESOLVED));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.ON_HOLD, EnumSet.of(ComplaintStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.RESOLVED, EnumSet.of(ComplaintStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.CLOSED, EnumSet.noneOf(ComplaintStatus.class));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.REJECTED, EnumSet.noneOf(ComplaintStatus.class));
    }

    private final ComplaintDAO complaintDAO;

    public ComplaintService(ComplaintDAO complaintDAO) {
        this.complaintDAO = complaintDAO;
    }

    /** Validates input, checks for an obvious duplicate, generates an ID and saves a new SUBMITTED complaint. */
    public synchronized Complaint createComplaint(String title, String description, String category,
                                                   String location, Priority priority, User submittedBy)
            throws InvalidComplaintException, DuplicateComplaintException, SQLException {
        InputValidator.validateComplaintInput(title, description, category);
        if (priority == null) {
            throw new InvalidComplaintException("A priority level is required.");
        }
        rejectIfLikelyDuplicate(title, submittedBy.getUserId());

        String id = ComplaintIdGenerator.nextId();
        Complaint complaint = new Complaint(id, title.trim(), description.trim(), category,
                location == null ? null : location.trim(), priority, submittedBy.getUserId());

        complaintDAO.save(complaint);
        recordHistory(complaint, null, ComplaintStatus.SUBMITTED, submittedBy.getName(), "Complaint submitted");
        LoggerUtil.log(submittedBy.getName(), "COMPLAINT_CREATED", id + " - " + title);
        return complaint;
    }

    // same title from the same person in the last couple of minutes almost certainly means
    // a double-submit (double click, retry after a slow response, etc) rather than a new issue
    private void rejectIfLikelyDuplicate(String title, int submittedBy) throws SQLException, DuplicateComplaintException {
        List<Complaint> recent = complaintDAO.findBySubmitter(submittedBy);
        for (Complaint existing : recent) {
            boolean sameTitle = existing.getTitle().equalsIgnoreCase(title.trim());
            boolean recentlyCreated = Duration.between(existing.getCreatedAt(), LocalDateTime.now()).toMinutes() < 2;
            if (sameTitle && recentlyCreated) {
                throw new DuplicateComplaintException("You already submitted \"" + title + "\" a moment ago.");
            }
        }
    }

    public Complaint getComplaint(String complaintId) throws SQLException, ComplaintNotFoundException {
        return complaintDAO.findById(complaintId);
    }

    public List<Complaint> getComplaintsForStudent(int userId) throws SQLException {
        return complaintDAO.findBySubmitter(userId);
    }

    public List<Complaint> getComplaintsForStaff(int staffId) throws SQLException {
        return complaintDAO.findByAssignee(staffId);
    }

    public List<Complaint> getAllComplaints() throws SQLException {
        return complaintDAO.findAll();
    }

    /** Highest priority (and oldest) complaints first, using Complaint's natural ordering. */
    public TreeSet<Complaint> getComplaintsSortedByPriority() throws SQLException {
        TreeSet<Complaint> sorted = new TreeSet<>();
        sorted.addAll(complaintDAO.findAll());
        return sorted;
    }

    public void changeStatus(String complaintId, ComplaintStatus newStatus, User changedBy, String remarks)
            throws SQLException, ComplaintNotFoundException, InvalidStatusTransitionException, UnauthorizedActionException {
        Complaint complaint = complaintDAO.findById(complaintId);
        ComplaintStatus current = complaint.getStatus();

        if (changedBy.getRole() == Role.STUDENT) {
            throw new UnauthorizedActionException("Students cannot change complaint status directly.");
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(ComplaintStatus.class)).contains(newStatus)) {
            throw new InvalidStatusTransitionException(current, newStatus);
        }

        complaint.updateStatus(newStatus);
        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        complaintDAO.update(complaint);
        recordHistory(complaint, current, newStatus, changedBy.getName(), remarks);
        LoggerUtil.log(changedBy.getName(), "STATUS_CHANGED", complaintId + " " + current + " -> " + newStatus);
    }

    public void resolveComplaint(String complaintId, User staff, String resolutionRemarks)
            throws SQLException, ComplaintNotFoundException, InvalidStatusTransitionException,
            UnauthorizedActionException, InvalidComplaintException {
        if (staff.getRole() != Role.STAFF && staff.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Only staff or admins can resolve a complaint.");
        }
        if (InputValidator.isBlank(resolutionRemarks)) {
            throw new InvalidComplaintException("Resolution remarks cannot be empty.");
        }
        Complaint complaint = complaintDAO.findById(complaintId);
        complaint.setResolutionRemarks(resolutionRemarks.trim());
        complaintDAO.update(complaint);
        changeStatus(complaintId, ComplaintStatus.RESOLVED, staff, resolutionRemarks.trim());
        LoggerUtil.log(staff.getName(), "COMPLAINT_RESOLVED", complaintId);
    }

    public void closeComplaint(String complaintId, User closedBy)
            throws SQLException, ComplaintNotFoundException, InvalidStatusTransitionException, UnauthorizedActionException {
        changeStatus(complaintId, ComplaintStatus.CLOSED, closedBy, "Complaint closed");
        LoggerUtil.log(closedBy.getName(), "COMPLAINT_CLOSED", complaintId);
    }

    /** A student can withdraw their own complaint while it's still early in the workflow. */
    public void cancelComplaint(String complaintId, User student)
            throws SQLException, ComplaintNotFoundException, UnauthorizedActionException, InvalidComplaintException {
        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint.getSubmittedBy() != student.getUserId()) {
            throw new UnauthorizedActionException("You can only cancel complaints you submitted.");
        }
        EnumSet<ComplaintStatus> cancellable = EnumSet.of(ComplaintStatus.SUBMITTED, ComplaintStatus.UNDER_REVIEW);
        if (!cancellable.contains(complaint.getStatus())) {
            throw new InvalidComplaintException("This complaint is already being worked on and can no longer be cancelled.");
        }
        ComplaintStatus previous = complaint.getStatus();
        complaint.updateStatus(ComplaintStatus.REJECTED);
        complaintDAO.update(complaint);
        recordHistory(complaint, previous, ComplaintStatus.REJECTED, student.getName(), "Cancelled by student");
        LoggerUtil.log(student.getName(), "COMPLAINT_CANCELLED", complaintId);
    }

    public List<ComplaintHistory> getHistory(String complaintId) throws SQLException {
        return complaintDAO.findHistory(complaintId);
    }

    /** Writes the current complaint list out to a CSV backup via NIO.2. */
    public void backupToFile() throws SQLException {
        FileManager.backupComplaints(complaintDAO.findAll());
    }

    private void recordHistory(Complaint complaint, ComplaintStatus oldStatus, ComplaintStatus newStatus,
                                String changedBy, String remarks) throws SQLException {
        complaintDAO.insertHistory(new ComplaintHistory(complaint.getComplaintId(), oldStatus, newStatus, changedBy, remarks));
    }
}
