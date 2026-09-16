package com.campusfix.service;

import com.campusfix.dao.ComplaintDAO;
import com.campusfix.model.Complaint;
import com.campusfix.model.ComplaintStatus;
import com.campusfix.model.Priority;
import com.campusfix.model.Reportable;
import com.campusfix.util.FileManager;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pulls the full complaint list and derives every number the admin dashboard needs with streams:
 * counts by status/category/priority, resolution rate, average time to resolve. Nothing here is
 * persisted separately - it's recomputed fresh every time it's asked for.
 */
public class AnalyticsService implements Reportable {

    private final ComplaintDAO complaintDAO;

    public AnalyticsService(ComplaintDAO complaintDAO) {
        this.complaintDAO = complaintDAO;
    }

    public long totalComplaints() throws SQLException {
        return snapshot().size();
    }

    public Map<ComplaintStatus, Long> countByStatus() throws SQLException {
        return snapshot().stream()
                .collect(Collectors.groupingBy(Complaint::getStatus, Collectors.counting()));
    }

    public Map<String, Long> countByCategory() throws SQLException {
        return snapshot().stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting()));
    }

    public Map<Priority, Long> countByPriority() throws SQLException {
        return snapshot().stream()
                .collect(Collectors.groupingBy(Complaint::getPriority, Collectors.counting()));
    }

    public List<Complaint> pendingComplaints() throws SQLException {
        return snapshot().stream()
                .filter(c -> c.getStatus() != ComplaintStatus.RESOLVED
                        && c.getStatus() != ComplaintStatus.CLOSED
                        && c.getStatus() != ComplaintStatus.REJECTED)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    public double resolutionRatePercent() throws SQLException {
        List<Complaint> all = snapshot();
        if (all.isEmpty()) return 0.0;
        long resolvedOrClosed = all.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.CLOSED)
                .count();
        return (resolvedOrClosed * 100.0) / all.size();
    }

    /** Average time between creation and resolution, in hours, across complaints that have a resolvedAt. */
    public double averageResolutionHours() throws SQLException {
        List<Complaint> resolved = snapshot().stream()
                .filter(c -> c.getResolvedAt() != null)
                .collect(Collectors.toList());
        if (resolved.isEmpty()) return 0.0;
        double totalHours = resolved.stream()
                .mapToDouble(c -> Duration.between(c.getCreatedAt(), c.getResolvedAt()).toMinutes() / 60.0)
                .sum();
        return totalHours / resolved.size();
    }

    // Reportable#generateReport() declares no checked exceptions, so a DB failure is caught
    // here and folded into the report text rather than propagated up to the caller.
    @Override
    public String generateReport() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("CampusFix Analytics Report\n");
            sb.append("Generated : ").append(LocalDateTime.now()).append("\n");
            sb.append("=".repeat(40)).append("\n\n");
            sb.append("Total Complaints        : ").append(totalComplaints()).append("\n");

            Map<ComplaintStatus, Long> byStatus = countByStatus();
            for (ComplaintStatus status : ComplaintStatus.values()) {
                sb.append(String.format("%-24s : %d%n", status, byStatus.getOrDefault(status, 0L)));
            }

            sb.append(String.format("%nResolution Rate         : %.2f%%%n", resolutionRatePercent()));
            sb.append(String.format("Average Resolution Time : %.1f hours%n", averageResolutionHours()));

            sb.append("\nBy Category\n");
            countByCategory().forEach((category, count) -> sb.append(String.format("  %-22s : %d%n", category, count)));

            sb.append("\nBy Priority\n");
            countByPriority().forEach((priority, count) -> sb.append(String.format("  %-22s : %d%n", priority, count)));

            return sb.toString();
        } catch (SQLException e) {
            return "Could not generate the analytics report - database error: " + e.getMessage();
        }
    }

    /** Writes the report to data/reports/ via NIO.2 and returns where it was saved. */
    public Path generateReportFile() {
        return FileManager.writeReport("analytics_report", generateReport());
    }

    private List<Complaint> snapshot() throws SQLException {
        return complaintDAO.findAll();
    }
}
