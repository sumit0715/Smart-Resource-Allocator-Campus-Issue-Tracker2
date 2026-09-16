package com.campusfix.dao;

import com.campusfix.exception.ComplaintNotFoundException;
import com.campusfix.model.Complaint;
import com.campusfix.model.ComplaintHistory;
import com.campusfix.model.ComplaintStatus;
import com.campusfix.model.Priority;
import com.campusfix.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ComplaintDAO {

    public void save(Complaint c) throws SQLException {
        String sql = "INSERT INTO complaints (complaint_id, title, description, category, location, "
                + "priority, status, submitted_by, assigned_to, department_id, created_at, updated_at, "
                + "resolved_at, resolution_remarks) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, c.getComplaintId());
            bindCommonFields(ps, c, i);
            ps.executeUpdate();
        }
    }

    public void update(Complaint c) throws SQLException {
        String sql = "UPDATE complaints SET title=?, description=?, category=?, location=?, priority=?, "
                + "status=?, submitted_by=?, assigned_to=?, department_id=?, created_at=?, updated_at=?, "
                + "resolved_at=?, resolution_remarks=? WHERE complaint_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int next = bindCommonFields(ps, c, 1);
            ps.setString(next, c.getComplaintId());
            ps.executeUpdate();
        }
    }

    /** Binds title..resolution_remarks starting at column startIndex. Returns the next free column. */
    private int bindCommonFields(PreparedStatement ps, Complaint c, int startIndex) throws SQLException {
        int i = startIndex;
        ps.setString(i++, c.getTitle());
        ps.setString(i++, c.getDescription());
        ps.setString(i++, c.getCategory());
        ps.setString(i++, c.getLocation());
        ps.setString(i++, c.getPriority().name());
        ps.setString(i++, c.getStatus().name());
        ps.setInt(i++, c.getSubmittedBy());
        if (c.getAssignedTo() != null) {
            ps.setInt(i++, c.getAssignedTo());
        } else {
            ps.setNull(i++, java.sql.Types.INTEGER);
        }
        if (c.getDepartmentId() != null) {
            ps.setInt(i++, c.getDepartmentId());
        } else {
            ps.setNull(i++, java.sql.Types.INTEGER);
        }
        ps.setTimestamp(i++, Timestamp.valueOf(c.getCreatedAt()));
        ps.setTimestamp(i++, Timestamp.valueOf(c.getUpdatedAt()));
        ps.setTimestamp(i++, c.getResolvedAt() == null ? null : Timestamp.valueOf(c.getResolvedAt()));
        ps.setString(i++, c.getResolutionRemarks());
        return i;
    }

    public Complaint findById(String complaintId) throws SQLException, ComplaintNotFoundException {
        String sql = "SELECT * FROM complaints WHERE complaint_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        throw new ComplaintNotFoundException(complaintId);
    }

    public List<Complaint> findAll() throws SQLException {
        String sql = "SELECT * FROM complaints ORDER BY created_at DESC";
        List<Complaint> results = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    public List<Complaint> findBySubmitter(int userId) throws SQLException {
        return queryByInt("SELECT * FROM complaints WHERE submitted_by = ? ORDER BY created_at DESC", userId);
    }

    public List<Complaint> findByAssignee(int userId) throws SQLException {
        return queryByInt("SELECT * FROM complaints WHERE assigned_to = ? ORDER BY created_at DESC", userId);
    }

    private List<Complaint> queryByInt(String sql, int value) throws SQLException {
        List<Complaint> results = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    public void insertHistory(ComplaintHistory h) throws SQLException {
        String sql = "INSERT INTO complaint_history (complaint_id, old_status, new_status, changed_by, "
                + "remarks) VALUES (?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, h.getComplaintId());
            ps.setString(2, h.getOldStatus() == null ? null : h.getOldStatus().name());
            ps.setString(3, h.getNewStatus().name());
            ps.setString(4, h.getChangedBy());
            ps.setString(5, h.getRemarks());
            ps.executeUpdate();
        }
    }

    public List<ComplaintHistory> findHistory(String complaintId) throws SQLException {
        String sql = "SELECT * FROM complaint_history WHERE complaint_id = ? ORDER BY changed_at";
        List<ComplaintHistory> results = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String oldStatusStr = rs.getString("old_status");
                    ComplaintStatus oldStatus = oldStatusStr == null ? null : ComplaintStatus.valueOf(oldStatusStr);
                    ComplaintHistory h = new ComplaintHistory(
                            rs.getString("complaint_id"), oldStatus,
                            ComplaintStatus.valueOf(rs.getString("new_status")),
                            rs.getString("changed_by"), rs.getString("remarks"));
                    h.setHistoryId(rs.getInt("history_id"));
                    results.add(h);
                }
            }
        }
        return results;
    }

    private Complaint mapRow(ResultSet rs) throws SQLException {
        Complaint c = new Complaint(
                rs.getString("complaint_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("location"),
                Priority.valueOf(rs.getString("priority")),
                rs.getInt("submitted_by"));

        c.updateStatus(ComplaintStatus.valueOf(rs.getString("status")));
        c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        c.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        int assignedTo = rs.getInt("assigned_to");
        if (!rs.wasNull()) {
            c.setAssignedTo(assignedTo);
        }
        int deptId = rs.getInt("department_id");
        if (!rs.wasNull()) {
            c.setDepartmentId(deptId);
        }
        Timestamp resolved = rs.getTimestamp("resolved_at");
        if (resolved != null) {
            c.setResolvedAt(resolved.toLocalDateTime());
        }
        c.setResolutionRemarks(rs.getString("resolution_remarks"));
        return c;
    }
}
