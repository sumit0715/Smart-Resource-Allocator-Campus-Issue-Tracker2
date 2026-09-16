package com.campusfix;

import com.campusfix.dao.ComplaintDAO;
import com.campusfix.dao.DepartmentDAO;
import com.campusfix.dao.FeedbackDAO;
import com.campusfix.dao.UserDAO;
import com.campusfix.exception.ComplaintNotFoundException;
import com.campusfix.exception.DepartmentNotFoundException;
import com.campusfix.exception.DuplicateComplaintException;
import com.campusfix.exception.InvalidComplaintException;
import com.campusfix.exception.InvalidStatusTransitionException;
import com.campusfix.exception.UnauthorizedActionException;
import com.campusfix.exception.UserNotFoundException;
import com.campusfix.model.Complaint;
import com.campusfix.model.ComplaintHistory;
import com.campusfix.model.ComplaintStatus;
import com.campusfix.model.Department;
import com.campusfix.model.Priority;
import com.campusfix.model.Role;
import com.campusfix.model.Staff;
import com.campusfix.model.Student;
import com.campusfix.model.User;
import com.campusfix.service.AnalyticsService;
import com.campusfix.service.AssignmentService;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.FeedbackService;
import com.campusfix.concurrency.ComplaintSubmissionTask;
import com.campusfix.util.FileManager;
import com.campusfix.util.InputValidator;
import com.campusfix.util.LoggerUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Console entry point for CampusFix. Everything here is thin plumbing - the actual
 * business logic lives in the service classes, which is what keeps this file readable
 * even though it covers three different role-based menus.
 */
public class Main {

    private static final Scanner IN = new Scanner(System.in);

    private static final ComplaintDAO complaintDAO = new ComplaintDAO();
    private static final DepartmentDAO departmentDAO = new DepartmentDAO();
    private static final UserDAO userDAO = new UserDAO();
    private static final FeedbackDAO feedbackDAO = new FeedbackDAO();

    private static final ComplaintService complaintService = new ComplaintService(complaintDAO);
    private static final AssignmentService assignmentService =
            new AssignmentService(complaintDAO, departmentDAO, userDAO, complaintService);
    private static final FeedbackService feedbackService = new FeedbackService(feedbackDAO, complaintDAO);
    private static final AnalyticsService analyticsService = new AnalyticsService(complaintDAO);

    public static void main(String[] args) {
        FileManager.ensureDataFolders();
        System.out.println("=========================================");
        System.out.println("   CampusFix - Campus Complaint System");
        System.out.println("=========================================");
        checkDatabase();

        boolean running = true;
        while (running) {
            System.out.println("\n1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");
            switch (IN.nextLine().trim()) {
                case "1" -> login();
                case "2" -> register();
                case "0" -> running = false;
                default -> System.out.println("That's not a valid option.");
            }
        }
        System.out.println("Goodbye.");
    }

    private static void checkDatabase() {
        try (var ignored = com.campusfix.util.DBConnection.getConnection()) {
            System.out.println("Connected to the campusfix database.");
        } catch (SQLException e) {
            System.out.println("Warning: could not reach the database (" + e.getMessage() + ")");
            System.out.println("Set CAMPUSFIX_DB_URL / CAMPUSFIX_DB_USER / CAMPUSFIX_DB_PASSWORD, "
                    + "or run database/campusfix.sql against a local MySQL instance first.");
        }
    }

    // ---------------------------------------------------------------- auth

    private static void register() {
        System.out.println("\n--- Register ---");
        System.out.print("Full name: ");
        String name = IN.nextLine().trim();
        System.out.print("Email: ");
        String email = IN.nextLine().trim();
        if (!InputValidator.isValidEmail(email)) {
            System.out.println("That doesn't look like a valid email address.");
            return;
        }
        System.out.print("Password: ");
        String password = IN.nextLine();
        System.out.print("Register as (1) Student or (2) Staff: ");
        String choice = IN.nextLine().trim();

        try {
            if (userDAO.emailExists(email)) {
                System.out.println("An account with that email already exists.");
                return;
            }
            User newUser;
            if ("2".equals(choice)) {
                Integer departmentId = pickDepartment();
                newUser = new Staff(0, name, email, password, departmentId);
            } else {
                newUser = new Student(0, name, email, password);
            }
            User saved = userDAO.save(newUser);
            LoggerUtil.log(saved.getName(), "LOGIN", "Registered a new " + saved.getRole() + " account");
            System.out.println("Account created. You can now log in as " + saved.getEmail());
        } catch (SQLException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private static Integer pickDepartment() throws SQLException {
        List<Department> departments = departmentDAO.findAll();
        System.out.println("Departments:");
        for (Department d : departments) {
            System.out.println("  " + d);
        }
        System.out.print("Department ID: ");
        int id = InputValidator.parseIntSafe(IN.nextLine(), -1);
        return departments.stream().anyMatch(d -> d.getDepartmentId() == id) ? id : null;
    }

    private static void login() {
        System.out.println("\n--- Login ---");
        System.out.print("Email: ");
        String email = IN.nextLine().trim();
        System.out.print("Password: ");
        String password = IN.nextLine();

        try {
            User user = userDAO.findByEmailAndPassword(email, password);
            LoggerUtil.log(user.getName(), "LOGIN", user.getRole() + " logged in");
            routeToDashboard(user);
        } catch (UserNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void routeToDashboard(User user) {
        switch (user.getRole()) {
            case STUDENT -> studentMenu(user);
            case STAFF -> staffMenu(user);
            case ADMIN -> adminMenu(user);
        }
    }

    // ------------------------------------------------------------- student

    private static void studentMenu(User student) {
        boolean loggedIn = true;
        while (loggedIn) {
            student.showDashboard();
            System.out.println("0. Log out");
            System.out.print("Choose an option: ");
            switch (IN.nextLine().trim()) {
                case "1" -> submitComplaint(student);
                case "2" -> trackComplaints(student);
                case "3" -> viewHistory();
                case "4" -> submitFeedback(student);
                case "5" -> cancelComplaint(student);
                case "0" -> loggedIn = false;
                default -> System.out.println("That's not a valid option.");
            }
        }
    }

    private static void submitComplaint(User student) {
        System.out.println("\nCategories: " + InputValidator.VALID_CATEGORIES);
        System.out.print("Title: ");
        String title = IN.nextLine();
        System.out.print("Description: ");
        String description = IN.nextLine();
        System.out.print("Category: ");
        String category = IN.nextLine().trim();
        System.out.print("Location (optional): ");
        String location = IN.nextLine();
        Priority priority = askPriority();

        try {
            Complaint complaint = complaintService.createComplaint(title, description, category, location, priority, student);
            System.out.println("\nComplaint ID : " + complaint.getComplaintId());
            System.out.println("Status       : " + complaint.getStatus());
        } catch (InvalidComplaintException | DuplicateComplaintException e) {
            System.out.println("Could not submit complaint: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static Priority askPriority() {
        System.out.print("Priority (LOW / MEDIUM / HIGH / CRITICAL): ");
        while (true) {
            try {
                return Priority.valueOf(IN.nextLine().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.print("Please enter one of LOW, MEDIUM, HIGH, CRITICAL: ");
            }
        }
    }

    private static void trackComplaints(User student) {
        try {
            List<Complaint> mine = complaintService.getComplaintsForStudent(student.getUserId());
            if (mine.isEmpty()) {
                System.out.println("You haven't submitted any complaints yet.");
                return;
            }
            mine.forEach(System.out::println);
            System.out.print("\nEnter a complaint ID to see full details (or press Enter to go back): ");
            String id = IN.nextLine().trim();
            if (!id.isEmpty()) {
                Complaint c = complaintService.getComplaint(id);
                System.out.println("\n" + c.generateReport());
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (ComplaintNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void submitFeedback(User student) {
        System.out.print("Complaint ID: ");
        String id = IN.nextLine().trim();
        System.out.print("Rating (1-5): ");
        int rating = InputValidator.parseIntSafe(IN.nextLine(), -1);
        System.out.print("Comment (optional): ");
        String comment = IN.nextLine();

        try {
            feedbackService.submitFeedback(id, student, rating, comment);
            System.out.println("Thanks - your feedback has been recorded.");
        } catch (ComplaintNotFoundException | InvalidComplaintException | UnauthorizedActionException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void cancelComplaint(User student) {
        System.out.print("Complaint ID to cancel: ");
        String id = IN.nextLine().trim();
        try {
            complaintService.cancelComplaint(id, student);
            System.out.println("Complaint " + id + " has been cancelled.");
        } catch (ComplaintNotFoundException | UnauthorizedActionException | InvalidComplaintException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // --------------------------------------------------------------- staff

    private static void staffMenu(User staff) {
        boolean loggedIn = true;
        while (loggedIn) {
            staff.showDashboard();
            System.out.println("0. Log out");
            System.out.print("Choose an option: ");
            switch (IN.nextLine().trim()) {
                case "1" -> viewAssigned(staff);
                case "2" -> acceptComplaint(staff);
                case "3" -> holdOrResume(staff);
                case "4" -> resolveComplaint(staff);
                case "5" -> viewHistory();
                case "0" -> loggedIn = false;
                default -> System.out.println("That's not a valid option.");
            }
        }
    }

    private static void viewAssigned(User staff) {
        try {
            List<Complaint> assigned = complaintService.getComplaintsForStaff(staff.getUserId());
            if (assigned.isEmpty()) {
                System.out.println("Nothing assigned to you right now.");
            } else {
                assigned.forEach(System.out::println);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void acceptComplaint(User staff) {
        System.out.print("Complaint ID to accept: ");
        String id = IN.nextLine().trim();
        try {
            assignmentService.acceptComplaint(id, staff);
            System.out.println("Complaint " + id + " is now IN_PROGRESS.");
        } catch (ComplaintNotFoundException | UnauthorizedActionException | InvalidStatusTransitionException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void holdOrResume(User staff) {
        System.out.print("Complaint ID: ");
        String id = IN.nextLine().trim();
        System.out.print("Put (H)old or (R)esume: ");
        String choice = IN.nextLine().trim().toUpperCase();
        ComplaintStatus target = choice.startsWith("H") ? ComplaintStatus.ON_HOLD : ComplaintStatus.IN_PROGRESS;
        System.out.print("Remarks: ");
        String remarks = IN.nextLine();
        try {
            complaintService.changeStatus(id, target, staff, remarks);
            System.out.println("Complaint " + id + " is now " + target + ".");
        } catch (ComplaintNotFoundException | InvalidStatusTransitionException | UnauthorizedActionException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void resolveComplaint(User staff) {
        System.out.print("Complaint ID to resolve: ");
        String id = IN.nextLine().trim();
        System.out.print("Resolution remarks: ");
        String remarks = IN.nextLine();
        try {
            complaintService.resolveComplaint(id, staff, remarks);
            System.out.println("Complaint " + id + " marked RESOLVED.");
        } catch (ComplaintNotFoundException | InvalidStatusTransitionException
                 | UnauthorizedActionException | InvalidComplaintException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void viewHistory() {
        System.out.print("Complaint ID: ");
        String id = IN.nextLine().trim();
        try {
            List<ComplaintHistory> history = complaintService.getHistory(id);
            if (history.isEmpty()) {
                System.out.println("No history found for that complaint.");
            } else {
                history.forEach(System.out::println);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // --------------------------------------------------------------- admin

    private static void adminMenu(User admin) {
        boolean loggedIn = true;
        while (loggedIn) {
            admin.showDashboard();
            System.out.println("0. Log out");
            System.out.print("Choose an option: ");
            switch (IN.nextLine().trim()) {
                case "1" -> viewAllComplaints();
                case "2" -> assignComplaint(admin);
                case "3" -> changePriority(admin);
                case "4" -> viewAnalytics();
                case "5" -> generateReportAndBackup();
                case "6" -> simulateConcurrentSubmissions();
                case "0" -> loggedIn = false;
                default -> System.out.println("That's not a valid option.");
            }
        }
    }

    private static void viewAllComplaints() {
        try {
            var sorted = complaintService.getComplaintsSortedByPriority();
            if (sorted.isEmpty()) {
                System.out.println("No complaints in the system yet.");
            } else {
                sorted.forEach(System.out::println);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void assignComplaint(User admin) {
        System.out.print("Complaint ID: ");
        String id = IN.nextLine().trim();
        System.out.print("(A)uto-route to a department, or assign a (S)taff member directly: ");
        String choice = IN.nextLine().trim().toUpperCase();
        try {
            if (choice.startsWith("A")) {
                assignmentService.autoAssignDepartment(id, admin);
                System.out.println("Routed automatically based on category.");
            } else {
                System.out.print("Staff user ID: ");
                int staffId = InputValidator.parseIntSafe(IN.nextLine(), -1);
                assignmentService.assignStaff(id, staffId, admin);
                System.out.println("Assigned.");
            }
        } catch (ComplaintNotFoundException | DepartmentNotFoundException | UserNotFoundException
                 | UnauthorizedActionException | InvalidStatusTransitionException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void changePriority(User admin) {
        System.out.print("Complaint ID: ");
        String id = IN.nextLine().trim();
        Priority priority = askPriority();
        try {
            Complaint complaint = complaintService.getComplaint(id);
            complaint.setPriority(priority);
            complaintDAO.update(complaint);
            LoggerUtil.log(admin.getName(), "PRIORITY_CHANGED", id + " -> " + priority);
            System.out.println("Priority updated.");
        } catch (ComplaintNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void viewAnalytics() {
        System.out.println("\n" + analyticsService.generateReport());
    }

    private static void generateReportAndBackup() {
        try {
            complaintService.backupToFile();
            var path = analyticsService.generateReportFile();
            System.out.println("Backup written to data/complaints_backup.csv");
            System.out.println("Report written to " + path);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    /** Fires off several ComplaintSubmissionTasks at once to demonstrate the ExecutorService / synchronized ID generation. */
    private static void simulateConcurrentSubmissions() {
        System.out.println("Submitting 5 sample complaints concurrently using student accounts 1-3...");
        ExecutorService pool = Executors.newFixedThreadPool(5);
        String[] titles = {"Wi-Fi down in hostel block B", "Leaking tap in washroom", "Projector not turning on",
                "Broken chair in lecture hall", "Flickering lights in library"};
        String[] categories = {"Wi-Fi/Network", "Water", "Classroom Equipment", "Furniture", "Library"};

        try {
            for (int i = 0; i < titles.length; i++) {
                int studentId = (i % 3) + 1;
                User student = userDAO.findById(studentId);
                Priority priority = i % 2 == 0 ? Priority.HIGH : Priority.MEDIUM;
                pool.submit(new ComplaintSubmissionTask(complaintService, student, titles[i],
                        "Auto-generated for the concurrency demo.", categories[i], "Campus", priority));
            }
        } catch (UserNotFoundException | SQLException e) {
            System.out.println("Demo setup failed - make sure the seed data from campusfix.sql has been loaded: "
                    + e.getMessage());
        }

        pool.shutdown();
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Demo complete - check option 1 to see the new complaints.");
    }
}
