package com.campusfix.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ComplaintHistory {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    private int historyId;
    private final String complaintId;
    private final ComplaintStatus oldStatus;
    private final ComplaintStatus newStatus;
    private final String changedBy;
    private final String remarks;
    private final LocalDateTime changedAt;

    public ComplaintHistory(String complaintId, ComplaintStatus oldStatus, ComplaintStatus newStatus,
                             String changedBy, String remarks) {
        this.complaintId = complaintId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.remarks = remarks;
        this.changedAt = LocalDateTime.now();
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getHistoryId() {
        return historyId;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public ComplaintStatus getOldStatus() {
        return oldStatus;
    }

    public ComplaintStatus getNewStatus() {
        return newStatus;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    @Override
    public String toString() {
        String from = oldStatus == null ? "(new)" : oldStatus.toString();
        return String.format("%-12s -> %-12s | by %-12s | %s%s",
                from, newStatus, changedBy, changedAt.format(TIME_FMT),
                (remarks == null || remarks.isBlank()) ? "" : " | " + remarks);
    }
}
