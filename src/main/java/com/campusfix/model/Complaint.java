package com.campusfix.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Complaint implements Trackable, Reportable, Comparable<Complaint> {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final String complaintId;
    private String title;
    private String description;
    private final String category;
    private final String location;
    private Priority priority;
    private ComplaintStatus status;
    private final int submittedBy;
    private Integer assignedTo;
    private Integer departmentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String resolutionRemarks;

    public Complaint(String complaintId, String title, String description, String category,
                      String location, Priority priority, int submittedBy) {
        this.complaintId = complaintId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.priority = priority;
        this.submittedBy = submittedBy;
        this.status = ComplaintStatus.SUBMITTED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @Override
    public void updateStatus(ComplaintStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public ComplaintStatus getStatus() {
        return status;
    }

    @Override
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Complaint ID : ").append(complaintId).append('\n');
        sb.append("Title        : ").append(title).append('\n');
        sb.append("Category     : ").append(category).append('\n');
        sb.append("Location     : ").append(location == null ? "-" : location).append('\n');
        sb.append("Priority     : ").append(priority).append('\n');
        sb.append("Status       : ").append(status).append('\n');
        sb.append("Created      : ").append(createdAt.format(DISPLAY_FMT)).append('\n');
        sb.append("Last Updated : ").append(updatedAt.format(DISPLAY_FMT)).append('\n');
        if (resolvedAt != null) {
            sb.append("Resolved     : ").append(resolvedAt.format(DISPLAY_FMT)).append('\n');
        }
        if (resolutionRemarks != null && !resolutionRemarks.isBlank()) {
            sb.append("Resolution   : ").append(resolutionRemarks).append('\n');
        }
        return sb.toString();
    }

    @Override
    public int compareTo(Complaint other) {
        int byPriority = other.priority.ordinal() - this.priority.ordinal();
        if (byPriority != 0) {
            return byPriority;
        }
        int byDate = this.createdAt.compareTo(other.createdAt);
        if (byDate != 0) {
            return byDate;
        }
        return this.complaintId.compareTo(other.complaintId);
    }

    public String getComplaintId() {
        return complaintId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }

    public int getSubmittedBy() {
        return submittedBy;
    }

    public Integer getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Integer assignedTo) {
        this.assignedTo = assignedTo;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionRemarks() {
        return resolutionRemarks;
    }

    public void setResolutionRemarks(String resolutionRemarks) {
        this.resolutionRemarks = resolutionRemarks;
    }

    @Override
    public String toString() {
        return complaintId + " | " + title + " | " + category + " | " + priority + " | " + status;
    }
}
