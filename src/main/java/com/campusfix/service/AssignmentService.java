package com.campusfix.service;

import com.campusfix.dao.ComplaintDAO;
import com.campusfix.dao.DepartmentDAO;
import com.campusfix.dao.UserDAO;
import com.campusfix.exception.ComplaintNotFoundException;
import com.campusfix.exception.DepartmentNotFoundException;
import com.campusfix.exception.InvalidStatusTransitionException;
import com.campusfix.exception.UnauthorizedActionException;
import com.campusfix.exception.UserNotFoundException;
import com.campusfix.model.Complaint;
import com.campusfix.model.ComplaintStatus;
import com.campusfix.model.Department;
import com.campusfix.model.Role;
import com.campusfix.model.Staff;
import com.campusfix.model.User;
import com.campusfix.util.LoggerUtil;

import java.sql.SQLException;
import java.util.Map;

/**
 * Routes complaints to a department (auto-suggested from the category, or picked manually)
 * and then to a specific staff member. Also covers a staff member accepting their assignment.
 */
public class AssignmentService {

    // category -> department name, mirrors the routing table in the project spec
    private static final Map<String, String> CATEGORY_TO_DEPARTMENT = Map.ofEntries(
            Map.entry("Electrical", "Electrical Maintenance"),
            Map.entry("Wi-Fi/Network", "IT Support"),
            Map.entry("Classroom Equipment", "IT Support"),
            Map.entry("Hostel", "Hostel Administration"),
            Map.entry("Cleanliness", "Housekeeping"),
            Map.entry("Water", "Housekeeping"),
            Map.entry("Furniture", "General Maintenance"),
            Map.entry("Library", "Library Administration"),
            Map.entry("Security", "Campus Security"),
            Map.entry("Transportation", "Transport Office"),
            Map.entry("Other", "General Maintenance")
    );

    private final ComplaintDAO complaintDAO;
    private final DepartmentDAO departmentDAO;
    private final UserDAO userDAO;
    private final ComplaintService complaintService;

    public AssignmentService(ComplaintDAO complaintDAO, DepartmentDAO departmentDAO, UserDAO userDAO,
                              ComplaintService complaintService) {
        this.complaintDAO = complaintDAO;
        this.departmentDAO = departmentDAO;
        this.userDAO = userDAO;
        this.complaintService = complaintService;
    }

    public static String suggestDepartmentName(String category) {
        return CATEGORY_TO_DEPARTMENT.getOrDefault(category, "General Maintenance");
    }

    /** Auto-routes a complaint to a department based on its category, then moves it to UNDER_REVIEW. */
    public void autoAssignDepartment(String complaintId, User admin)
            throws SQLException, ComplaintNotFoundException, DepartmentNotFoundException,
            UnauthorizedActionException, InvalidStatusTransitionException {
        requireAdmin(admin);
        Complaint complaint = complaintDAO.findById(complaintId);
        Department department = departmentDAO.findByName(suggestDepartmentName(complaint.getCategory()));
        complaint.setDepartmentId(department.getDepartmentId());
        complaintDAO.update(complaint);
        complaintService.changeStatus(complaintId, ComplaintStatus.UNDER_REVIEW, admin,
                "Routed to " + department.getName());
        LoggerUtil.log(admin.getName(), "COMPLAINT_ASSIGNED", complaintId + " -> dept " + department.getName());
    }

    public void assignDepartmentManually(String complaintId, int departmentId, User admin)
            throws SQLException, ComplaintNotFoundException, DepartmentNotFoundException, UnauthorizedActionException {
        requireAdmin(admin);
        Department department = departmentDAO.findById(departmentId);
        Complaint complaint = complaintDAO.findById(complaintId);
        complaint.setDepartmentId(department.getDepartmentId());
        complaintDAO.update(complaint);
        LoggerUtil.log(admin.getName(), "COMPLAINT_ASSIGNED", complaintId + " -> dept " + department.getName());
    }

    public void assignStaff(String complaintId, int staffUserId, User admin)
            throws SQLException, ComplaintNotFoundException, UserNotFoundException,
            UnauthorizedActionException, InvalidStatusTransitionException {
        requireAdmin(admin);
        User candidate = userDAO.findById(staffUserId);
        if (candidate.getRole() != Role.STAFF) {
            throw new UnauthorizedActionException(candidate.getName() + " is not a staff member.");
        }
        Staff staff = (Staff) candidate;
        Complaint complaint = complaintDAO.findById(complaintId);
        complaint.setAssignedTo(staff.getUserId());
        if (complaint.getDepartmentId() == null && staff.getDepartmentId() != null) {
            complaint.setDepartmentId(staff.getDepartmentId());
        }
        complaintDAO.update(complaint);
        complaintService.changeStatus(complaintId, ComplaintStatus.ASSIGNED, admin,
                "Assigned to " + staff.getName());
        LoggerUtil.log(admin.getName(), "COMPLAINT_ASSIGNED", complaintId + " -> staff " + staff.getName());
    }

    /** Staff accepting their assignment moves the complaint into IN_PROGRESS. */
    public void acceptComplaint(String complaintId, User staff)
            throws SQLException, ComplaintNotFoundException, UnauthorizedActionException, InvalidStatusTransitionException {
        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint.getAssignedTo() == null || complaint.getAssignedTo() != staff.getUserId()) {
            throw new UnauthorizedActionException("This complaint isn't assigned to you.");
        }
        complaintService.changeStatus(complaintId, ComplaintStatus.IN_PROGRESS, staff, "Accepted by staff");
    }

    private void requireAdmin(User user) throws UnauthorizedActionException {
        if (user.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Only an administrator can perform assignments.");
        }
    }
}
