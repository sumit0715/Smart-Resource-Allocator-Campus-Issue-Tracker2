package com.campusfix.dao;

import com.campusfix.model.Feedback;
import com.campusfix.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDAO {

    public void save(Feedback feedback) throws SQLException {
        String sql = "INSERT INTO feedback (complaint_id, user_id, rating, comment) VALUES (?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, feedback.getComplaintId());
            ps.setInt(2, feedback.getUserId());
            ps.setInt(3, feedback.getRating());
            ps.setString(4, feedback.getComment());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    feedback.setFeedbackId(keys.getInt(1));
                }
            }
        }
    }

    public List<Feedback> findByComplaintId(String complaintId) throws SQLException {
        String sql = "SELECT * FROM feedback WHERE complaint_id = ?";
        List<Feedback> results = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Feedback f = new Feedback(rs.getString("complaint_id"), rs.getInt("user_id"),
                            rs.getInt("rating"), rs.getString("comment"));
                    f.setFeedbackId(rs.getInt("feedback_id"));
                    results.add(f);
                }
            }
        }
        return results;
    }

    public double averageRating() throws SQLException {
        String sql = "SELECT AVG(rating) AS avg_rating FROM feedback";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }
        }
        return 0.0;
    }
}
